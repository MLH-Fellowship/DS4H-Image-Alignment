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
import ds4h.dialog.main.event.IMainDialogEvent;
import ds4h.image.manager.ImagesManager;
import ij.IJ;
import ij.ImagePlus;
import ij.VirtualStack;
import ij.io.FileSaver;

import java.awt.*;
import java.awt.image.ColorModel;
import java.io.File;
import java.util.List;

public abstract class AbstractBuilder {
  protected static final String TEMP_PATH = "temp";
  protected static final String TIFF_EXT = ".tiff";
  protected static final String IMAGE_SIZE_TOO_BIG = "During computation the expected file size overcame imagej file limit. To continue, deselect \"keep all pixel data\" option.";
  protected static final String IMAGE_SIZE_TOO_BIG_TITLE = "Error: image size too big";
  
  private final LoadingDialog loadingDialog;
  private final OnAlignDialogEventListener listener;
  private final ImagesManager manager;
  private final IMainDialogEvent event;
  
  private AlignDialog alignDialog;
  private VirtualStack virtualStack;
  private ImagePlus transformedImagesStack;
  private Dimension maximumSize;
  private List<String> tempImages;
  
  public AbstractBuilder(LoadingDialog loadingDialog, OnAlignDialogEventListener listener, ImagesManager manager, IMainDialogEvent event) {
    this.loadingDialog = loadingDialog;
    this.listener = listener;
    this.manager = manager;
    this.event = event;
  }
  
  public abstract void init();
  
  public abstract void align();
  
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
    this.maximumSize = maximumSize;
  }
  
  protected ImagePlus getTransformedImagesStack() {
    return this.transformedImagesStack;
  }
  
  protected void setTransformedImagesStack(ImagePlus transformedImagesStack) {
    this.transformedImagesStack = transformedImagesStack;
  }
  
  protected IMainDialogEvent getEvent() {
    return this.event;
  }
  
  protected void setVirtualStack() {
    this.virtualStack = new VirtualStack(this.getMaximumSize().width, this.getMaximumSize().height, ColorModel.getRGBdefault(), IJ.getDir(TEMP_PATH));
  }
}
