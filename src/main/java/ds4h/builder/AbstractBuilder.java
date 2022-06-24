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
import ds4h.dialog.main.event.MainDialogEvent;
import ds4h.image.buffered.BufferedImage;
import ds4h.image.manager.ImagesManager;
import ij.IJ;
import ij.ImagePlus;
import ij.VirtualStack;
import ij.gui.Roi;
import ij.io.FileSaver;

import java.awt.*;
import java.awt.image.ColorModel;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractBuilder {
  protected static final String TEMP_PATH = "temp";
  protected static final String TIFF_EXT = ".tiff";
  protected static final String IMAGE_SIZE_TOO_BIG = "During computation the expected file size overcame imagej file limit. To continue, deselect \"keep all pixel data\" option.";
  protected static final String IMAGE_SIZE_TOO_BIG_TITLE = "Error: image size too big";
  
  private final LoadingDialog loadingDialog;
  private final OnAlignDialogEventListener listener;
  private final ImagesManager manager;
  private final MainDialogEvent event;
  
  private AlignDialog alignDialog;
  private VirtualStack virtualStack;
  private ImagePlus transformedImagesStack;
  private Dimension maximumSize;
  private List<Integer> offsetsX;
  private List<Integer> offsetsY;
  private Integer maxOffsetX;
  private Integer maxOffsetY;
  private int maxOffsetXIndex;
  private int maxOffsetYIndex;
  private int edgeX;
  private int edgeY;
  private int edgeX2;
  private int edgeY2;
  private List<String> tempImages = new ArrayList<>();
  private List<Dimension> imagesDimensions;
  private int sourceImageIndex = -1;

  
  protected AbstractBuilder(LoadingDialog loadingDialog, OnAlignDialogEventListener listener, ImagesManager manager, MainDialogEvent event) {
    this.loadingDialog = loadingDialog;
    this.listener = listener;
    this.manager = manager;
    this.event = event;
    this.imagesDimensions = new ArrayList<>(manager.getImagesDimensions());
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

  public abstract void build();
  
  protected void addToVirtualStack(ImagePlus img, VirtualStack virtualStack) {
    String path = IJ.getDir(TEMP_PATH) + img.getProcessor().hashCode() + TIFF_EXT;
    new FileSaver(img).saveAsTiff(path);
    virtualStack.addSlice(new File(path).getName());
    this.tempImages.add(path);
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
  
  protected ImagesManager getManager() {
    return this.manager;
  }
  
  protected VirtualStack getVirtualStack() {
    return this.virtualStack;
  }
  
  protected Dimension getMaximumSize() {
    return this.maximumSize;
  }
  
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
  
  public MainDialogEvent getEvent() {
    return this.event;
  }
  
  protected void setVirtualStack() {
    this.virtualStack = new VirtualStack(this.getMaximumSize().width, this.getMaximumSize().height, ColorModel.getRGBdefault(), IJ.getDir(TEMP_PATH));
  }

  public int getSourceImageIndex() {
    return sourceImageIndex;
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

  public List<Integer> getOffsetsY() {
    return offsetsY;
  }

  public Integer getMaxOffsetX() {
    return maxOffsetX;
  }

  public Integer getMaxOffsetY() {
    return maxOffsetY;
  }

  public int getMaxOffsetXIndex() {
    return maxOffsetXIndex;
  }

  public int getMaxOffsetYIndex() {
    return maxOffsetYIndex;
  }

  public int getEdgeX() {
    return edgeX;
  }

  public int getEdgeY() {
    return edgeY;
  }

  public int getEdgeX2() {
    return edgeX2;
  }

  public int getEdgeY2() {
    return edgeY2;
  }

  public void setOffsetsX(List<Integer> offsetsX) {
    this.offsetsX = offsetsX;
  }

  public void setOffsetsY(List<Integer> offsetsY) {
    this.offsetsY = offsetsY;
  }

  public void setMaxOffsetX(Integer maxOffsetX) {
    this.maxOffsetX = maxOffsetX;
  }

  public void setMaxOffsetY(Integer maxOffsetY) {
    this.maxOffsetY = maxOffsetY;
  }

  public void setMaxOffsetXIndex(int maxOffsetXIndex) {
    this.maxOffsetXIndex = maxOffsetXIndex;
  }

  public void setMaxOffsetYIndex(int maxOffsetYIndex) {
    this.maxOffsetYIndex = maxOffsetYIndex;
  }

  public void setEdgeX(int edgeX) {
    this.edgeX = edgeX;
  }

  public void setEdgeY(int edgeY) {
    this.edgeY = edgeY;
  }

  public void setEdgeX2(int edgeX2) {
    this.edgeX2 = edgeX2;
  }

  public void setEdgeY2(int edgeY2) {
    this.edgeY2 = edgeY2;
  }


}
