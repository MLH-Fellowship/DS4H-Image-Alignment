package ds4h.image.manager;

import ds4h.image.buffered.BufferedImage;
import ds4h.image.model.ImageFile;
import ds4h.observer.Observable;
import ds4h.services.FileService;
import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.plugin.frame.RoiManager;
import loci.formats.FormatException;

import java.awt.*;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

/**
 * ListIterator of ImagePlus that has imagesFiles
 * If you find why does it make sense please explain it to me.
 * The previous developer has clearly not implemented a real iterator.
 * I want to finish my things, I don't want always to fix things, come on.
 */
public class ImagesManager implements ListIterator<ImagePlus>, Observable {
  // the YYYY-MM-DD format grants to the user the fact that the sorting can be done always by the name
  private final static String DATE_YMD_FORMAT = "yyyy-MM-dd";
  private final static String DATE_HMS_FORMAT = "HH-mm-ss";
  private final PropertyChangeSupport support = new PropertyChangeSupport(this);
  private final List<ImageFile> imageFiles = new ArrayList<>();
  private final List<ImageFile> originalImageFiles = new ArrayList<>();
  private int imageIndex;
  
  public ImagesManager(List<String> filesPath) throws ImageOversizeException, FormatException, IOException {
    this.imageIndex = -1;
    for (String filePath : filesPath) {
      this.addFile(filePath);
      this.addFileToOriginalList(filePath);
    }
  }
  
  public void addFile(String pathFile) throws IOException, FormatException {
    ImageFile imageFile = new ImageFile(pathFile);
    this.getImageFiles().add(imageFile);
    this.addListener(imageFile);
  }
  
  private void addFile(String pathFile, int index) throws IOException, FormatException {
    ImageFile imageFile = new ImageFile(pathFile);
    this.getImageFiles().add(index, imageFile);
    this.addListener(imageFile);
  }
  
  public void addFileToOriginalList(String path) throws IOException, FormatException {
    ImageFile imageFile = new ImageFile(path);
    this.getOriginalImageFiles().add(imageFile);
  }
  
  private void addListener(ImageFile file) {
    ImagePlus.addImageListener(new ImageListener() {
      @Override
      public void imageOpened(ImagePlus imagePlus) {
        // Nothing for now
      }
      
      @Override
      public void imageClosed(ImagePlus imagePlus) {
        // Nothing for now
      }
      
      @Override
      public void imageUpdated(ImagePlus imagePlus) {
        if (imagePlus.changes) {
          handleUpdatedImageChanges(imagePlus, file);
          // then remove it
          ImagePlus.removeImageListener(this);
        }
      }
    });
  }
  
  private void handleUpdatedImageChanges(ImagePlus imagePlus, ImageFile file) {
    String path = this.saveUpdatedImage(imagePlus, file);
    try {
      int indexToRemove = this.imageIndex;
      // delete from stack the old one
      this.removeImageFile(indexToRemove);
      // add new one
      this.addFile(path, indexToRemove);
      this.firePropertyChange("updatedImage", file.getPathFile(), path);
    } catch (IOException | FormatException e) {
      IJ.showMessage(e.getMessage());
    }
  }
  
  private String saveUpdatedImage(ImagePlus imagePlus, ImageFile file) {
    final String baseDir = this.getDirFromPath(file.getPathFile());
    // Thanks to this you can have a more organized folder
    final String todayDir = this.getTodayDate(DATE_YMD_FORMAT) + "/";
    FileService.createDirectoryIfNotExist(baseDir + todayDir);
    final String dir = baseDir + todayDir;
    String path = String.format("%s%d-%s.tiff", dir, imagePlus.getProcessor().hashCode(), this.getTodayDate(DATE_HMS_FORMAT));
    new FileSaver(imagePlus).saveAsTiff(path);
    return path;
  }
  
  private String getTodayDate(String pattern) {
    LocalDateTime dateObj = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
    return dateObj.format(formatter);
  }
  
  private String getDirFromPath(String path) {
    int lastIndexOfSeparator = path.lastIndexOf(File.separator);
    if (lastIndexOfSeparator != -1) {
      path = path.substring(0, lastIndexOfSeparator + 1);
    }
    return path;
  }
  
  private BufferedImage getImage(int index, boolean wholeSlide, boolean isOriginal) {
    int progressive = 0;
    ImageFile imageFile = null;
    List<ImageFile> imageFiles = isOriginal ? new ArrayList<>(this.getOriginalImageFiles()) : new ArrayList<>(this.getImageFiles());
    for (int i = 0; i < imageFiles.size(); i++) {
      if (progressive + imageFiles.get(i).getNImages() > index) {
        imageFile = imageFiles.get(i);
        break;
      }
      progressive += imageFiles.get(i).getNImages();
    }
    try {
      if (index == -1) {
        index = 0; // Just an ugly patch
      }
      if (imageFile != null) {
        final BufferedImage image = imageFile.getImage(index - progressive, wholeSlide);
        image.setFilePath(imageFile.getPathFile());
        if (!isOriginal) {
          image.setTitle(MessageFormat.format("Editor Image {0}/{1}", index + 1, this.getNImages()));
        }
        return image;
      }
    } catch (Exception e) {
      IJ.showMessage(e.getMessage());
    }
    return null;
  }
  
  @Override
  public boolean hasNext() {
    return this.getImageIndex() < this.getNImages() - 1;
  }
  
  @Override
  public BufferedImage next() {
    if (!hasNext()) {
      return null;
    }
    this.imageIndex++;
    return this.getImage(this.getImageIndex(), false, false);
  }
  
  @Override
  public boolean hasPrevious() {
    return this.getImageIndex() > 0;
  }
  
  @Override
  public BufferedImage previous() {
    if (!hasPrevious()) {
      return null;
    }
    this.imageIndex--;
    return getImage(this.getImageIndex(), false, false);
  }
  
  @Override
  public int nextIndex() {
    return this.imageIndex + 1;
  }
  
  @Override
  public int previousIndex() {
    return this.imageIndex - 1;
  }
  
  @Override
  public void remove() {
  }
  
  @Override
  public void set(ImagePlus imagePlus) {
  
  }
  
  @Override
  public void add(ImagePlus imagePlus) {
  }
  
  public int getCurrentIndex() {
    return this.imageIndex;
  }
  
  public int getNImages() {
    return this.getImageFiles().stream().mapToInt(ImageFile::getNImages).sum();
  }
  
  /**
   * This flag indicates whenever the manger uses a reduced-size image for compatibility
   */
  public void dispose() {
    this.getImageFiles().forEach(imageFile -> {
      try {
        imageFile.dispose();
      } catch (IOException e) {
        IJ.showMessage(e.getMessage());
      }
    });
  }
  
  public BufferedImage get(int index) {
    return this.getImage(index, false, false);
  }
  
  public BufferedImage get(int index, boolean wholeSlide) {
    return this.getImage(index, wholeSlide, false);
  }
  
  public BufferedImage getOriginal(int index, boolean wholeSlide) {
    return this.getImage(index, wholeSlide, true);
  }
  
  public List<RoiManager> getRoiManagers() {
    return this.getImageFiles()
          .stream()
          .map(ImageFile::getRoiManagers)
          .flatMap(List::stream)
          .collect(Collectors.toList());
  }
  
  // ?? Unused ??
  public Dimension getMaximumSize() {
    Dimension maximumSize = new Dimension();
    this.getImageFiles().forEach(imageFile -> {
      Dimension dimension = imageFile.getMaximumSize();
      maximumSize.width = (double) dimension.width > maximumSize.width ? dimension.width : maximumSize.width;
      maximumSize.height = (double) dimension.height > maximumSize.height ? dimension.height : maximumSize.height;
    });
    return maximumSize;
  }
  
  public List<Dimension> getImagesDimensions() {
    List<Dimension> dimensions;
    dimensions = this.getOriginalImageFiles().stream().reduce(new ArrayList<>(), (accDimensions, imageFile) -> {
      accDimensions.addAll(imageFile.getImagesDimensions());
      return accDimensions;
    }, (accumulated, value) -> accumulated);
    return dimensions;
  }
  
  public List<ImageFile> getImageFiles() {
    return this.imageFiles;
  }
  
  public List<ImageFile> getOriginalImageFiles() {
    return this.originalImageFiles;
  }
  
  /**
   * Remove the imageFile from the manager and updates the image index
   *
   * @param index
   */
  public void removeImageFile(int index) {
    this.getImageFiles().remove(index);
    this.imageIndex = this.getImageIndex() >= this.getNImages() ? index - 1 : index;
  }
  
  private int getImageIndex() {
    return this.imageIndex;
  }
  
  @Override
  public PropertyChangeSupport getSupport() {
    return this.support;
  }
  
  public static class ImageOversizeException extends Exception {
  }
}
