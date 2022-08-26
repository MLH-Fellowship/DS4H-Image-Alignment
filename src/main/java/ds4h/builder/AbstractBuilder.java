/*
 * COPYRIGHT:* Copyright (c) 2021, AUSL Romagna* Azienda USL della Romagna, Italy* All rights reserved.**
 * The informed consent published in integral part on the website of Azienda USL della Romagna
 * (Informed Consent AUSL, prot. N. 1683), must be citied.**
 * This material is free; you can redistribute it and/or modify it under the terms of the CC BY 4.0.*
 * This material is distributed WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package ds4h.builder;

import ds4h.dialog.align.AlignDialog;
import ds4h.dialog.align.OnAlignDialogEventListener;
import ds4h.dialog.loading.LoadingDialog;
import ds4h.dialog.main.event.RegistrationEvent;
import ds4h.image.model.manager.ImagesEditor;
import ij.IJ;
import ij.ImagePlus;
import ij.VirtualStack;
import ij.io.FileSaver;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.image.ColorModel;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractBuilder<T> implements AlignBuilder {
    protected static final String TEMP_PATH = "temp";
    protected static final String TIFF_EXT = ".tiff";
    protected static final String IMAGE_SIZE_TOO_BIG = "During computation the expected file size overcame imagej file limit. To continue, deselect \"keep all pixel data\" option.";
    protected static final String IMAGE_SIZE_TOO_BIG_TITLE = "Error: image size too big";
    private final LoadingDialog loadingDialog;
    private final OnAlignDialogEventListener listener;
    private final ImagesEditor editor;
    private final RegistrationEvent event;

    private AlignDialog alignDialog;
    private VirtualStack virtualStack;
    private ImagePlus transformedImagesStack;
    private Dimension maximumSize;

    private Dimension finalStack;
    private List<Integer> offsetsX;
    private List<Integer> offsetsY;
    private Integer maxOffsetX;
    private Integer maxOffsetY;
    private int maxOffsetXIndex;
    private int maxOffsetYIndex;
    private List<String> tempImages = new ArrayList<>();
    private List<Dimension> imagesDimensions;
    private int sourceImageIndex = -1;


    protected AbstractBuilder(LoadingDialog loadingDialog, OnAlignDialogEventListener listener, ImagesEditor editor, RegistrationEvent event) {
        this.loadingDialog = loadingDialog;
        this.listener = listener;
        this.editor = editor;
        this.event = event;
        this.imagesDimensions = new ArrayList<>(editor.getImagesDimensions());
        editor.loadImagesWholeSlides();
        editor.loadOriginalImagesWholeSlides();
    }

    public abstract void init();

    public abstract boolean check();

    /**
     * Unchecked checkbox "Keep all pixel data" method
     */
    public abstract void align();

    /**
     * Checked checkbox "Keep all pixel data" method
     */
    public abstract void alignKeepOriginal();

    protected abstract T getSourceImage();

    protected abstract ImageProcessor getFinalStackImageProcessor();

    public void build() {
        try {
            this.setTransformedImagesStack(new ImagePlus("", this.getVirtualStack()));
            String filePath = IJ.getDir(TEMP_PATH) + this.getTransformedImagesStack().hashCode() + TIFF_EXT;
            new ImageConverter(this.getTransformedImagesStack()).convertToGray8();
            new FileSaver(this.getTransformedImagesStack()).saveAsTiff(filePath);
            this.getTempImages().add(filePath);
            this.getLoadingDialog().hideDialog();
            this.setAlignDialog(new AlignDialog(this.getTransformedImagesStack(), this.getListener()));
            this.getAlignDialog().pack();
            this.getAlignDialog().setVisible(true);
        } catch (Exception e) {
            System.out.println(e.getMessage() + "  " + Arrays.toString(e.getStackTrace()));
        }
        this.getLoadingDialog().hideDialog();
    }

    protected void addToVirtualStack(ImagePlus img) {
        String path = IJ.getDir(TEMP_PATH) + img.getProcessor().hashCode() + TIFF_EXT;
        new FileSaver(img).saveAsTiff(path);
        this.getVirtualStack().addSlice(new File(path).getName());
        this.tempImages.add(path);
    }

    protected void checkFinalStackDimension() {
        // The final stack of the image is exceeding the maximum size of the images for imagej (see http://imagej.1557.x6.nabble.com/Large-image-td5015380.html)
        if (((double) this.getFinalStack().width * this.getFinalStack().height) > Integer.MAX_VALUE) {
            JOptionPane.showMessageDialog(null, IMAGE_SIZE_TOO_BIG, IMAGE_SIZE_TOO_BIG_TITLE, JOptionPane.ERROR_MESSAGE);
            this.getLoadingDialog().hideDialog(); // take care of this
        }
    }

    protected void setFinalStackToVirtualStack() {
        this.setVirtualStack(new VirtualStack(this.getFinalStack().width, this.getFinalStack().height, ColorModel.getRGBdefault(), IJ.getDir(TEMP_PATH)));
    }

    protected void addFinalStackToVirtualStack() {
        this.addToVirtualStack(new ImagePlus("", this.getFinalStackImageProcessor()));
    }

    public LoadingDialog getLoadingDialog() {
        return this.loadingDialog;
    }

    public List<String> getTempImages() {
        return this.tempImages;
    }

    public void setTempImages(List<String> tempImages) {
        this.tempImages = tempImages;
    }

    public AlignDialog getAlignDialog() {
        return this.alignDialog;
    }

    protected void setAlignDialog(AlignDialog alignDialog) {
        this.alignDialog = alignDialog;
    }

    protected OnAlignDialogEventListener getListener() {
        return this.listener;
    }

    protected ImagesEditor getEditor() {
        return this.editor;
    }

    protected VirtualStack getVirtualStack() {
        return this.virtualStack;
    }

    protected void setVirtualStack(VirtualStack virtualStack) {
        this.virtualStack = virtualStack;
    }

    protected Dimension getMaximumSize() {
        return this.maximumSize;
    }

    //takes input dimension and check if maximumSize needs to be updated
    protected void setMaximumSize(Dimension maximumSize) {
        for (int i = 0; i < this.getImagesDimensions().size(); i++) {
            Dimension dimension = this.getImagesDimensions().get(i);
            if (dimension.width > maximumSize.width) {
                maximumSize.width = dimension.width;
                this.sourceImageIndex = i;
            }
            if (dimension.height > maximumSize.height) {
                maximumSize.height = dimension.height;
            }
        }
        this.maximumSize = maximumSize;
    }

    protected ImagePlus getTransformedImagesStack() {
        return this.transformedImagesStack;
    }

    protected void setTransformedImagesStack(ImagePlus transformedImagesStack) {
        this.transformedImagesStack = transformedImagesStack;
    }

    public RegistrationEvent getEvent() {
        return this.event;
    }

    public int getSourceImageIndex() {
        return sourceImageIndex;
    }

    public void setSourceImageIndex(int sourceImageIndex) {
        this.sourceImageIndex = sourceImageIndex;
    }

    public List<Dimension> getImagesDimensions() {
        return imagesDimensions;
    }

    protected void setImagesDimensions(List<Dimension> imagesDimensions) {
        this.imagesDimensions = new ArrayList<>(imagesDimensions);
    }

    public List<Integer> getOffsetsX() {
        return offsetsX;
    }

    public void setOffsetsX(List<Integer> offsetsX) {
        this.offsetsX = offsetsX;
    }

    public List<Integer> getOffsetsY() {
        return offsetsY;
    }

    public void setOffsetsY(List<Integer> offsetsY) {
        this.offsetsY = offsetsY;
    }

    public Integer getMaxOffsetX() {
        return maxOffsetX;
    }

    public void setMaxOffsetX(Integer maxOffsetX) {
        this.maxOffsetX = maxOffsetX;
    }

    public Integer getMaxOffsetY() {
        return maxOffsetY;
    }

    public void setMaxOffsetY(Integer maxOffsetY) {
        this.maxOffsetY = maxOffsetY;
    }

    public int getMaxOffsetXIndex() {
        return maxOffsetXIndex;
    }

    public void setMaxOffsetXIndex(int maxOffsetXIndex) {
        this.maxOffsetXIndex = maxOffsetXIndex;
    }

    public int getMaxOffsetYIndex() {
        return maxOffsetYIndex;
    }

    public void setMaxOffsetYIndex(int maxOffsetYIndex) {
        this.maxOffsetYIndex = maxOffsetYIndex;
    }

    public Dimension getFinalStack() {
        return finalStack;
    }

    public void setFinalStack(Dimension finalStack) {
        this.finalStack = finalStack;
    }
}
