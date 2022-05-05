package ds4h.image.model;

import ds4h.image.buffered.BufferedImage;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.frame.RoiManager;
import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.gui.BufferedImageReader;
import loci.plugins.in.DisplayHandler;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class ImageFile {
  private final String pathFile;
  private final List<RoiManager> roiManagers;
  private boolean reducedImageMode;
  private Dimension editorImageDimension;
  private BufferedImageReader bufferedEditorImageReader;
  private BufferedImageReader bufferedEditorImageReaderWholeSlide;
  private ImportProcess importProcess;
  private boolean wholeSlideInitialized = false;
  private List<java.awt.image.BufferedImage> cached_thumbs;
  
  public ImageFile(String pathFile) throws IOException, FormatException {
    this.pathFile = pathFile;
    this.roiManagers = new ArrayList<>();
    this.generateImageReader();
  }
  
  public static long estimateMemoryUsage(String pathFile) throws IOException {
    return getImageImportingProcess(pathFile).getMemoryUsage();
  }
  
  private static ImportProcess getImageImportingProcess(String pathFile) throws IOException {
    ImporterOptions options = new ImporterOptions();
    options.setId(pathFile);
    options.setVirtual(true);
    options.setGroupFiles(false);
    options.setUngroupFiles(true);
    options.setOpenAllSeries(true);
    ImportProcess process = new ImportProcess(options);
    try {
      process.execute();
    } catch (Exception e) {
      IJ.showMessage(e.getMessage());
    }
    return process;
  }
  
  private void generateImageReader() throws FormatException, IOException {
    this.importProcess = getImageImportingProcess(pathFile);
    final IFormatReader reader = this.importProcess.getBaseReader();
    final ImagePlusReader imageReader = new ImagePlusReader(this.importProcess);
    final ImagePlus[] imps = imageReader.openImagePlus();
    final double realSize = Arrays.stream(imps).mapToDouble(ImagePlus::getSizeInBytes).sum();
    final double gb = realSize / (1 << 30); // 1 << 30 is the same as 1024^3
    final double maxGb = 2.0;
    if (gb > maxGb) {
      IJ.showMessage("IS GB OVER 2GB");
      this.reducedImageMode = true;
    }
    this.editorImageDimension = new Dimension(reader.getSizeX(), reader.getSizeY());
    this.bufferedEditorImageReader = BufferedImageReader.makeBufferedImageReader(reader);
    IntStream.range(0, bufferedEditorImageReader.getImageCount()).mapToObj(i -> new RoiManager(false)).forEachOrdered(this.roiManagers::add);
  }
  
  /**
   * Yeah, getNImages is get numbers of images,
   * because calling it getImagesCounter was too much
   * of a work and no, I don't want to change
   *
   * @return counter
   */
  public int getNImages() {
    return this.bufferedEditorImageReader.getImageCount();
  }
  
  public BufferedImage getImage(int index, boolean wholeSlide) throws IOException, FormatException {
    if (!wholeSlide) {
      return new BufferedImage("", bufferedEditorImageReader.openImage(index), roiManagers.get(index), reducedImageMode);
    }
    if (!wholeSlideInitialized) {
      try {
        getWholeSlideImage();
      } catch (Exception e) {
        IJ.showMessage(e.getMessage());
      }
    }
    return new BufferedImage("", bufferedEditorImageReaderWholeSlide.openImage(index), roiManagers.get(index), this.editorImageDimension);
  }
  
  public void dispose() throws IOException {
    bufferedEditorImageReader.close();
    roiManagers.forEach(Window::dispose);
  }
  
  private void getWholeSlideImage() throws IOException, FormatException {
    this.wholeSlideInitialized = true;
    // If the bufferedImageReader is already using the first series (thus the images with the biggest sizes) there is no need to initialize a new bufferedImageReader. We can just reuse it as it is
    if (bufferedEditorImageReader.getSeries() == 0) {
      this.bufferedEditorImageReaderWholeSlide = bufferedEditorImageReader;
      return;
    }
    DisplayHandler displayHandler = new DisplayHandler(importProcess);
    displayHandler.displayOriginalMetadata();
    displayHandler.displayOMEXML();
    this.bufferedEditorImageReaderWholeSlide = BufferedImageReader.makeBufferedImageReader(importProcess.getReader());
    this.bufferedEditorImageReaderWholeSlide.setSeries(0);
  }
  
  public List<RoiManager> getRoiManagers() {
    return this.roiManagers;
  }
  
  /**
   * Returns the maximum image size obtainable by the current ImageFile
   *
   * @return
   */
  public Dimension getMaximumSize() {
    Dimension maximumSize = new Dimension();
    for (int i = 0; i < importProcess.getReader().getSeriesCount(); i++) {
      importProcess.getReader().setSeries(i);
      maximumSize.width = Math.max(importProcess.getReader().getSizeX(), maximumSize.width);
      maximumSize.height = Math.max(importProcess.getReader().getSizeY(), maximumSize.height);
    }
    return maximumSize;
  }
  
  public ArrayList<Dimension> getImagesDimensions() {
    ArrayList<Dimension> dimensions = new ArrayList<>();
    for (int i = 0; i < importProcess.getReader().getSeriesCount(); i++) {
      importProcess.getReader().setSeries(i);
      dimensions.add(new Dimension(importProcess.getReader().getSizeX(), importProcess.getReader().getSizeY()));
    }
    return dimensions;
  }
  
  public List<java.awt.image.BufferedImage> getThumbs() {
    try {
      // lazy initialization
      if (this.cached_thumbs == null) {
        this.cached_thumbs = new ArrayList<>();
        for (int i = 0; i < bufferedEditorImageReader.getImageCount(); i++) {
          cached_thumbs.add(this.bufferedEditorImageReader.openThumbImage(i));
        }
      }
    } catch (FormatException | IOException e) {
      IJ.showMessage(e.getMessage());
    }
    return cached_thumbs;
  }
  
  public String getPathFile() {
    return this.pathFile;
  }
}
