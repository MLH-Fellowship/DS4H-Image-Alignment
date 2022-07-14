package ds4h.image.model.manager;

import ds4h.image.model.manager.slide.SlideImage;
import ds4h.observer.Observable;
import ds4h.services.FileService;
import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.io.FileSaver;
import loci.formats.FormatException;

import java.awt.*;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class ImagesEditor implements Observable {
    // the YYYY-MM-DD format grants to the user the fact that the sorting can be done always by the name
    private static final String DATE_YMD_FORMAT = "yyyy-MM-dd";
    private static final String DATE_HMS_FORMAT = "HH-mm-ss";
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private final List<ImageFile> imageFiles = new ArrayList<>();
    private final List<ImageFile> originalImageFiles = new ArrayList<>();
    private int currentPosition = -1;


    public ImagesEditor(List<String> filesPath) throws ImageOversizeException, FormatException, IOException {
        for (String filePath : filesPath) {
            this.addFile(filePath);
            this.addFileToOriginalList(filePath);
        }
    }

    public void addFile(String pathFile) throws IOException, FormatException {
        ImageFile imageFile = new ImageFile(pathFile);
        this.getImageFiles().add(imageFile);
        this.attachListener(imageFile);
    }

    private void addFile(String pathFile, int index) throws IOException, FormatException {
        ImageFile imageFile = new ImageFile(pathFile);
        this.getImageFiles().add(index, imageFile);
        this.attachListener(imageFile);
    }

    public void addFileToOriginalList(String path) throws IOException, FormatException {
        ImageFile imageFile = new ImageFile(path);
        this.getOriginalImageFiles().add(imageFile);
    }

    private void attachListener(ImageFile file) {
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
            int indexToRemove = this.getCurrentPosition();
            // delete from stack the old one
            this.removeImageFile(indexToRemove);
            // add new one
            this.addFile(path, indexToRemove);
            this.firePropertyChange("updatedImage", file.getPathFile(), path);
        } catch (IOException | FormatException e) {
            IJ.showMessage(e.getMessage());
        }
    }

    /**
     * Remove the imageFile from the manager and updates the image index
     *
     * @param index
     */
    public void removeImageFile(int index) {
        this.getImageFiles().remove(index);
        this.currentPosition = this.getCurrentPosition() >= this.getAllImagesCounterSum() ? this.getCurrentPosition() - 1 : this.getCurrentPosition();
    }

    public void removeOriginalImageFile(int imageFileIndex) {
        this.getOriginalImageFiles().remove(imageFileIndex);
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

    public List<ImageFile> getImageFiles() {
        return imageFiles;
    }

    public List<ImageFile> getOriginalImageFiles() {
        return originalImageFiles;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public int getAllImagesCounterSum() {
        return this.getImageFiles().stream().mapToInt(ImageFile::getImagesCounter).sum();
    }


    public boolean hasNext() {
        return this.getCurrentPosition() < this.getAllImagesCounterSum() - 1;
    }


    public void next() {
        if (hasNext()) {
            this.nextIndex();
        }
    }

    public SlideImage getCurrentImage() {
        // SlideImage image = this.getSlideImages().get(this.getCurrentPosition());
        return getImage(getCurrentPosition(), false, false);
    }

    public SlideImage getCurrentOriginalImage() {
        // SlideImage image = this.getOriginalSlideImages().get(this.getCurrentPosition());
        return getImage(getCurrentPosition(), false, true);
    }

    public SlideImage getOriginalWholeSlideImage(int index) {
        return this.getImage(index, true, true);
    }

    public SlideImage getWholeSlideImage(int index) {
        return this.getImage(index, true, false);
    }

    public SlideImage getOriginalSlideImage(int index) {
        return this.getImage(index, false, true);
    }

    public SlideImage getSlideImage(int index) {
        return this.getImage(index, false, false);
    }

    private SlideImage getImage(int index, boolean wholeSlide, boolean isOriginal) {
        int progressive = 0;
        ImageFile imageFile = null;
        List<ImageFile> imageFileList = isOriginal ? this.getOriginalImageFiles() : this.getImageFiles();
        for (ImageFile file : imageFileList) {
            if (progressive + file.getImagesCounter() > index) {
                imageFile = file;
                break;
            }
            progressive += file.getImagesCounter();
        }
        try {
            if (imageFile != null) {
                final SlideImage image = imageFile.getImage(index - progressive, wholeSlide);
                if (!isOriginal) {
                    image.setTitle(MessageFormat.format("Editor Image {0}/{1}", index - progressive, this.getAllImagesCounterSum()));
                }
                return image;
            }
        } catch (Exception e) {
            IJ.showMessage(e.getMessage());
        }
        return null;
    }


    public boolean hasPrevious() {
        return this.getCurrentPosition() > 0;
    }


    public void previous() {
        if (this.hasPrevious()) {
            this.previousIndex();
        }
    }


    public void nextIndex() {
        currentPosition++;
    }


    public void previousIndex() {
        currentPosition--;
    }

    public void dispose() {
        Stream.of(this.getImageFiles(), this.getOriginalImageFiles()).flatMap(Collection::parallelStream).forEachOrdered(imageFile -> {
            try {
                imageFile.dispose();
            } catch (IOException e) {
                IJ.showMessage(e.getMessage());
            }
        });
    }

    public List<Dimension> getImagesDimensions() {
        List<Dimension> dimensions;
        dimensions = this.getOriginalImageFiles().stream().reduce(new ArrayList<>(), (accDimensions, imageFile) -> {
            accDimensions.addAll(imageFile.getImagesDimensions());
            return accDimensions;
        }, (accumulated, value) -> accumulated);
        return dimensions;
    }


    public void loadImagesWholeSlides() {
        Stream.of(this.getImageFiles()).flatMap(Collection::parallelStream).filter(imageFile -> imageFile.getImagesWholeSlide().size() == 0).forEachOrdered(ImageFile::createImagesWholeSlide);
    }

    @Override
    public PropertyChangeSupport getSupport() {
        return support;
    }

    public void loadOriginalImagesWholeSlides() {
        Stream.of(this.getOriginalImageFiles()).flatMap(Collection::parallelStream).filter(imageFile -> imageFile.getImagesWholeSlide().size() == 0).forEachOrdered(ImageFile::createImagesWholeSlide);
    }

    /**
     * TODO: make it into an external class, maybe you'll choose a better way to handle exceptions
     */
    public static class ImageOversizeException extends Exception {
    }
}
