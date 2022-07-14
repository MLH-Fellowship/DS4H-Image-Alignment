package ds4h.image.model.manager;

import ds4h.image.model.manager.slide.SlideImage;
import ds4h.services.ImportService;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.frame.RoiManager;
import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.gui.BufferedImageReader;
import loci.plugins.in.DisplayHandler;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class ImageFile {
    private final String pathFile;
    List<SlideImage> slideImages = new ArrayList<>();
    List<SlideImage> imagesWholeSlide = new ArrayList<>();
    private boolean reducedImageMode;
    private Dimension editorImageDimension;
    private BufferedImageReader bufferedEditorImageReader;
    private BufferedImageReader bufferedEditorImageReaderWholeSlide;
    private ImportProcess importProcess;
    private boolean wholeSlideInitialized = false;
    private List<BufferedImage> cached_thumbs;

    public ImageFile(String pathFile) throws IOException, FormatException {
        this.pathFile = pathFile;
        this.generateImageReader();
    }

    public static long estimateMemoryUsage(String pathFile) throws IOException {
        return ImportService.getProcessByFilePath(pathFile).getMemoryUsage();
    }

    private void generateImageReader() throws FormatException, IOException {
        this.importProcess = ImportService.getProcessByFilePath(pathFile);
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
        if (!wholeSlideInitialized) {
            try {
                this.getWholeSlideImage();
            } catch (Exception e) {
                IJ.showMessage(e.getMessage());
            }
        }
        this.createImages();
    }

    /**
     * Yeah, getNImages is get numbers of images,
     * because calling it getImagesCounter was too much
     * of a work and no, I don't want to change
     *
     * @return counter
     */
    public int getImagesCounter() {
        return this.bufferedEditorImageReader.getImageCount();
    }

    public SlideImage getImage(int index, boolean isWholeSlide) {
        return isWholeSlide ? this.getImagesWholeSlide().get(index) : this.getImages().get(index);
    }

    private void createImages() {
        IntStream.range(0, bufferedEditorImageReader.getImageCount()).forEach(i -> {
            try {
                this.createImage(i);
            } catch (IOException | FormatException e) {
                IJ.showMessage(e.getMessage());
            }
        });
    }

    public void createImagesWholeSlide() {
        IntStream.range(0, bufferedEditorImageReader.getImageCount()).forEach(i -> {
            try {
                this.createImageWholeSlide(i);
            } catch (IOException | FormatException e) {
                IJ.showMessage(e.getMessage());
            }
        });
    }


    private void createImage(int index) throws IOException, FormatException {
        this.getImages().add(new SlideImage("", bufferedEditorImageReader.openImage(index), new RoiManager(false), this.getPathFile(), reducedImageMode));
    }

    private void createImageWholeSlide(int index) throws IOException, FormatException {
        this.getImagesWholeSlide().add(new SlideImage("", bufferedEditorImageReaderWholeSlide.openImage(index), this.getImages().get(index).getManager(), this.getPathFile(), this.editorImageDimension));
    }

    public void dispose() throws IOException {
        bufferedEditorImageReader.close();
        this.getImages().stream().map(SlideImage::getManager).forEach(Window::dispose);
        this.getImagesWholeSlide().stream().map(SlideImage::getManager).forEach(Window::dispose);
    }

    private void getWholeSlideImage() throws IOException, FormatException {
        this.wholeSlideInitialized = true;
        // If the bufferedImageReader is already using the first series (thus the images with the biggest sizes) there is no need to initialize a new bufferedImageReader. We can just reuse it as it is
        if (bufferedEditorImageReader.getSeries() == 0) {
            this.bufferedEditorImageReaderWholeSlide = bufferedEditorImageReader;
            return;
        }
        // These 3 lines of code ↓ , are taken from the Importer plugin, the fact is that I need to check if
        // They're actually doing something useful or not
        DisplayHandler displayHandler = new DisplayHandler(importProcess);
        displayHandler.displayOriginalMetadata();
        displayHandler.displayOMEXML();
        this.bufferedEditorImageReaderWholeSlide = BufferedImageReader.makeBufferedImageReader(importProcess.getReader());
        this.bufferedEditorImageReaderWholeSlide.setSeries(0);
    }

    /**
     * Returns the maximum image size obtainable by the current ImageFile
     *
     * @return
     */
    public Dimension getMaximumSize() {
        Dimension maximumSize = new Dimension();
        for (int i = 0; i < this.getImportProcess().getReader().getSeriesCount(); i++) {
            this.getImportProcess().getReader().setSeries(i);
            maximumSize.width = Math.max(this.getImportProcess().getReader().getSizeX(), maximumSize.width);
            maximumSize.height = Math.max(this.getImportProcess().getReader().getSizeY(), maximumSize.height);
        }
        return maximumSize;
    }

    public ArrayList<Dimension> getImagesDimensions() {
        ArrayList<Dimension> dimensions = new ArrayList<>();
        for (int i = 0; i < this.getImportProcess().getReader().getSeriesCount(); i++) {
            this.getImportProcess().getReader().setSeries(i);
            dimensions.add(new Dimension(this.getImportProcess().getReader().getSizeX(), this.getImportProcess().getReader().getSizeY()));
        }
        return dimensions;
    }


    private ImportProcess getImportProcess() {
        return this.importProcess;
    }

    public List<BufferedImage> getThumbs() {
        try {
            // lazy initialization
            if (this.cached_thumbs == null) {
                this.cached_thumbs = new ArrayList<>();
                for (int i = 0; i < bufferedEditorImageReader.getImageCount(); i++) {
                    this.cached_thumbs.add(this.bufferedEditorImageReader.openThumbImage(i));
                }
            }
        } catch (FormatException | IOException e) {
            IJ.showMessage(e.getMessage());
        }
        return this.cached_thumbs;
    }

    public List<SlideImage> getImages() {
        return slideImages;
    }

    public List<SlideImage> getImagesWholeSlide() {
        return imagesWholeSlide;
    }

    public String getPathFile() {
        return this.pathFile;
    }
}
