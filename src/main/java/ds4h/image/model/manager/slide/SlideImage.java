package ds4h.image.model.manager.slide;


import ds4h.observer.Observable;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

import java.awt.*;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Class that represents an image inside the DS4H alignment software.
 * Edo: I leave the Observable interface here, just because it could help you in some way
 * I leave to you the decision if you don't need it or not
 */
public class SlideImage extends ImagePlus implements Observable {
    private final RoiManager manager;
    private final boolean isReduced;
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private boolean copyCornersMode;
    private Roi[] roisBackup;
    private Dimension reducedImageDimensions;

    private OnSlideImageEventListener listener;
    private final String filePath;

    public SlideImage(String text, Image image, RoiManager manager, String filePath, boolean isReduced) {
        super(text, image);
        this.manager = manager;
        this.filePath = filePath;
        this.isReduced = isReduced;
    }

    public SlideImage(String text, Image image, RoiManager manager, String filePath, Dimension reduceImageDimensions) {
        super(text, image);
        this.manager = manager;
        this.filePath = filePath;
        this.isReduced = true;
        this.reducedImageDimensions = reduceImageDimensions;
    }

    public RoiManager getManager() {
        return this.manager;
    }

    public void restoreRois() {
        Arrays.stream(this.roisBackup).forEach(roi -> manager.add(this, roi, 0));
    }

    public void backupRois() {
        this.roisBackup = this.manager.getRoisAsArray();
    }

    public boolean isReduced() {
        return isReduced;
    }

    /**
     * I don't get where it can be used
     * @return simply the dimension (width, height) of the reduced version
     * Sorry but I don't get everything of what the previous had intended to do
     */
    public Dimension getEditorImageDimension() {
        return reducedImageDimensions;
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public PropertyChangeSupport getSupport() {
        return support;
    }

    public boolean isCopyCornersMode() {
        return this.copyCornersMode;
    }

    public void setCopyCornersMode(boolean isCopyCornersMode) {
        this.copyCornersMode = isCopyCornersMode;
        IntStream.range(0, this.getManager().getRoisAsArray().length).forEachOrdered(roi -> this.getManager().select(roi, isCopyCornersMode, isCopyCornersMode));
    }

    public OnSlideImageEventListener getListener() {
        return listener;
    }

    public void setListener(OnSlideImageEventListener listener) {
        this.listener = listener;
    }
}