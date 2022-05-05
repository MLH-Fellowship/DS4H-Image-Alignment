/*
 * COPYRIGHT:* Copyright (c) 2021, AUSL Romagna* Azienda USL della Romagna, Italy* All rights reserved.**
 * The informed consent published in integral part on the website of Azienda USL della Romagna
 * (Informed Consent AUSL, prot. N. 1683), must be citied.**
 * This material is free; you can redistribute it and/or modify it under the terms of the CC BY 4.0.*
 * This material is distributed WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package ds4h.image.registration;

import ds4h.builder.AbstractBuilder;
import ds4h.builder.FREAKBuilder;
import ds4h.builder.LeastSquareTransformationBuilder;
import ds4h.dialog.about.AboutDialog;
import ds4h.dialog.align.AlignDialog;
import ds4h.dialog.align.OnAlignDialogEventListener;
import ds4h.dialog.align.event.IAlignDialogEvent;
import ds4h.dialog.align.event.ReuseImageEvent;
import ds4h.dialog.align.event.SaveEvent;
import ds4h.dialog.loading.LoadingDialog;
import ds4h.dialog.main.MainDialog;
import ds4h.dialog.main.OnMainDialogEventListener;
import ds4h.dialog.main.event.*;
import ds4h.dialog.preview.OnPreviewDialogEventListener;
import ds4h.dialog.preview.PreviewDialog;
import ds4h.dialog.preview.event.CloseDialogEvent;
import ds4h.dialog.preview.event.IPreviewDialogEvent;
import ds4h.dialog.remove.OnRemoveDialogEventListener;
import ds4h.dialog.remove.RemoveImageDialog;
import ds4h.dialog.remove.event.IRemoveDialogEvent;
import ds4h.image.buffered.BufferedImage;
import ds4h.image.manager.ImagesManager;
import ds4h.image.model.ImageFile;
import ds4h.services.FileService;
import ds4h.services.loader.Loader;
import ds4h.utils.Pair;
import ds4h.utils.Utilities;
import ij.IJ;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.plugin.frame.RoiManager;
import loci.common.enumeration.EnumException;
import loci.formats.UnknownFormatException;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.MessageFormat;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ImageAlignment implements OnMainDialogEventListener, OnPreviewDialogEventListener, OnAlignDialogEventListener, OnRemoveDialogEventListener, PropertyChangeListener {
  private static final String IMAGES_SCALED_MESSAGE = "Image size too large: image has been scaled for compatibility.";
  private static final String SINGLE_IMAGE_MESSAGE = "Only one image detected in the stack: align operation will be unavailable.";
  private static final String IMAGES_OVERSIZE_MESSAGE = "Cannot open the selected image: image exceed supported dimensions.";
  private static final String ALIGNED_IMAGE_NOT_SAVED_MESSAGE = "Aligned images not saved: are you sure you want to exit without saving?";
  private static final String DELETE_ALL_IMAGES = "Do you confirm to delete all the images of the stack?";
  private static final String IMAGE_SAVED_MESSAGE = "Image successfully saved";
  private static final String INSUFFICIENT_MEMORY_MESSAGE = "Insufficient computer memory (RAM) available. \n\n\t Try to increase the allocated memory by going to \n\n\t                Edit  ▶ Options  ▶ Memory & Threads \n\n\t Change \"Maximum Memory\" to, at most, 1000 MB less than your computer's total RAM.";
  private static final String INSUFFICIENT_MEMORY_TITLE = "Error: insufficient memory";
  private static final String UNKNOWN_FORMAT_MESSAGE = "Error: trying to open a file with a unsupported format.";
  private static final String UNKNOWN_FORMAT_TITLE = "Error: unknown format";
  private static final String MAIN_DIALOG_TITLE_PATTERN = "Editor Image {0}/{1}";
  private static final String CAREFUL_NOW_TITLE = "Careful now";
  private static final String TIFF_EXT = ".tiff";
  private static final String NULL_PATH = "nullnull";
  private static final String TOO_FEW_CORNER_POINTS = "Remember that if you want to align via corners, you should have the same numbers of Corners for each image, and they should be at least 3";
  private static final String DELETE_COMMAND = "Delete";
  
  static {
    ImageAlignment.loader();
  }
  
  private List<String> tempImages = new ArrayList<>();
  private ImagesManager manager;
  private BufferedImage image = null;
  private BufferedImage originalImage = null;
  private MainDialog mainDialog;
  private PreviewDialog previewDialog;
  private AlignDialog alignDialog;
  private LoadingDialog loadingDialog;
  private AboutDialog aboutDialog;
  private RemoveImageDialog removeImageDialog;
  private boolean alignedImageSaved = false;
  private long totalMemory = 0;
  
  private static void loader() {
    Loader loader = new Loader();
    loader.load();
  }
  
  @Override
  public void onMainDialogEvent(IMainDialogEvent dialogEvent) {
    if (this.image != null) {
      WindowManager.setCurrentWindow(this.image.getWindow());
    }
    switch (dialogEvent.getClass().getSimpleName()) {
      case "PreviewImageEvent":
        this.previewImage((PreviewImageEvent) dialogEvent);
        break;
      case "ChangeImageEvent":
        this.getChangeImageThread((ChangeImageEvent) dialogEvent);
        break;
      case "DeleteRoiEvent":
        this.deleteRoi((DeleteRoiEvent) dialogEvent);
        break;
      case "DeleteRoisEvent":
        this.deleteRois((DeleteRoisEvent) dialogEvent);
        break;
      case "AddRoiEvent":
        this.addRoi((AddRoiEvent) dialogEvent);
        break;
      case "SelectedRoisEvent":
        this.handleSelectedRois((SelectedRoisEvent) dialogEvent);
        break;
      case "SelectedRoiEvent":
        this.handleSelectedRoi((SelectedRoiEvent) dialogEvent);
        break;
      case "SelectedRoiFromOvalEvent":
        this.handleSelectedRoiFromOval((SelectedRoiFromOvalEvent) dialogEvent);
        break;
      case "DeselectedRoiEvent":
        this.handleDeselectedRoi((DeselectedRoiEvent) dialogEvent);
        break;
      case "AlignEvent":
        this.align((AlignEvent) dialogEvent);
        break;
      case "AutoAlignEvent":
        this.autoAlign((AutoAlignEvent) dialogEvent);
        break;
      case "OpenFileEvent":
      case "ExitEvent":
        this.openOrExitEventHandler(dialogEvent);
        break;
      case "OpenAboutEvent":
        this.aboutDialog.setVisible(true);
        break;
      case "MovedRoiEvent":
        this.handleMovedRoi();
        break;
      case "AddFileEvent":
        this.addFile((AddFileEvent) dialogEvent);
        break;
      case "CopyCornersEvent":
        this.copyCorners();
        break;
      case "RemoveImageEvent":
        if (this.getRemoveImageDialog() == null || !this.getRemoveImageDialog().isVisible()) {
          this.removeImage();
        }
        break;
      default:
        IJ.showMessage("No event known was called");
        break;
    }
  }
  
  private void handleMovedRoi() {
    this.getMainDialog().refreshROIList(this.getImage().getManager());
    if (this.getPreviewDialog() != null) this.getPreviewDialog().drawRois();
  }
  
  private void removeImage() {
    this.getLoadingDialog().showDialog();
    Utilities.setTimeout(() -> {
      this.removeImageDialog = new RemoveImageDialog(this.manager.getImageFiles(), this);
      this.getRemoveImageDialog().setVisible(true);
      this.getLoadingDialog().hideDialog();
      this.getLoadingDialog().requestFocus();
    }, 20);
  }
  
  private void openOrExitEventHandler(IMainDialogEvent dialogEvent) {
    boolean roisPresent = this.getManager().getRoiManagers().stream().anyMatch(it -> it.getRoisAsArray().length != 0);
    if (roisPresent && handleRoisPresence(dialogEvent)) {
      if (dialogEvent instanceof OpenFileEvent) {
        String pathFile = promptForFile();
        if (!pathFile.equals(NULL_PATH)) {
          this.disposeAll();
          this.initialize(Collections.singletonList(pathFile));
        }
      }
      if (dialogEvent instanceof ExitEvent) {
        this.disposeAll();
        System.exit(0);
      }
    }
  }
  
  private boolean handleRoisPresence(IMainDialogEvent dialogEvent) {
    String[] buttons = {"Yes", "No"};
    String message = dialogEvent instanceof OpenFileEvent ? "This will replace the existing " + this.getImage().getFileInfo().fileName + ". Proceed anyway?" : "You will lose the existing added landmarks. Proceed anyway?";
    int answer = JOptionPane.showOptionDialog(null, message, CAREFUL_NOW_TITLE, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, buttons, buttons[1]);
    return answer != 1;
  }
  
  private void copyCorners() {
    // get the indexes of all roi managers with at least a roi added
    List<Integer> imageIndexes;
    // remove the index of the current image, if present.
    List<Integer> list = new ArrayList<>();
    Roi[] roisOfCurrentImage = this.getImage().getManager().getRoisAsArray();
    for (RoiManager roiManager : this.getManager().getRoiManagers()) {
      if (roiManager.getRoisAsArray().length == 0) {
        continue;
      }
      int index = this.getManager().getRoiManagers().indexOf(roiManager);
      if (index != this.getManager().getCurrentIndex()) {
        list.add(index);
      }
    }
    imageIndexes = list;
    Object[] options = imageIndexes.stream().map(imageIndex -> "Image " + (imageIndex + 1)).toArray();
    JComboBox<Object> optionList = new JComboBox<>(options);
    optionList.setSelectedIndex(0);
    // n means most certainly "answer"
    int n = JOptionPane.showOptionDialog(new JFrame(), optionList, "Copy from", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Copy", "Cancel"}, JOptionPane.YES_OPTION);
    if (n == JOptionPane.YES_OPTION) {
      BufferedImage chosenImage = this.getManager().get(optionList.getSelectedIndex());
      double ratioOfChosenImage = this.getImageRatio(chosenImage);
      double ratioOfCurrentImage = this.getImageRatio(this.getImage());
      RoiManager selectedManager = this.getManager().getRoiManagers().get(imageIndexes.get(optionList.getSelectedIndex()));
      Map<Integer, Pair<BigDecimal, BigDecimal>> roiPoints = this.handleRoiInNewImage(roisOfCurrentImage, ratioOfChosenImage, ratioOfCurrentImage, selectedManager);
      // I'll definitely have to refactor all this duplicated code hanging around
      if (!roiPoints.isEmpty()) {
        this.handleRoiPointsThatWereNotAdded(chosenImage, roiPoints);
      }
      this.refreshRoiGUI();
      this.getImage().setCopyCornersMode();
    }
  }
  
  private Map<Integer, Pair<BigDecimal, BigDecimal>> handleRoiInNewImage(Roi[] roisOfCurrentImage, double ratioOfChosenImage, double ratioOfCurrentImage, RoiManager selectedManager) {
    Map<Integer, Pair<BigDecimal, BigDecimal>> roiPoints = new HashMap<>();
    Roi[] rois = selectedManager.getRoisAsArray();
    for (int index = 0; index < rois.length; index++) {
      final Roi roi = rois[index];
      final BigDecimal x = BigDecimal.valueOf(roi.getRotationCenter().xpoints[0]);
      final BigDecimal y = BigDecimal.valueOf(roi.getRotationCenter().ypoints[0]);
      Pair<BigDecimal, BigDecimal> point = this.convertPointRatio(new Pair<>(x, y), ratioOfChosenImage, ratioOfCurrentImage);
      if (this.isAlreadyThere(point, roisOfCurrentImage)) {
        continue;
      }
      if (point.getX().intValue() < this.getImage().getWidth() && point.getY().intValue() < this.getImage().getHeight()) {
        this.onMainDialogEvent(new AddRoiEvent(point));
        continue;
      }
      roiPoints.put(index, point);
    }
    return roiPoints;
  }
  
  private void handleRoiPointsThatWereNotAdded(BufferedImage chosenImage, Map<Integer, Pair<BigDecimal, BigDecimal>> roiPoints) {
    String[] buttons = {"Yes", "No"};
    String message = "The corners out of bounds are deleted. Do you want to delete the same corners in the referenced image?";
    int answer = JOptionPane.showOptionDialog(null, message, CAREFUL_NOW_TITLE, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, buttons, buttons[1]);
    if (answer == 0) {
      int[] roisIndices = roiPoints.keySet().stream().mapToInt(value -> value).toArray();
      // Keep in mind that when you want to delete multiple rois, it should be done from in reverse order
      IntStream.range(0, roisIndices.length).map(i -> roisIndices[roisIndices.length - 1 - i]).forEachOrdered(index -> {
        chosenImage.getManager().select(index);
        if (chosenImage.getManager().isSelected(index)) {
          chosenImage.getManager().runCommand(DELETE_COMMAND);
        }
      });
    }
    this.warnUserIfNumberOfCornersIsNotTheSame(chosenImage, image);
  }
  
  private boolean isAlreadyThere(Pair<BigDecimal, BigDecimal> point, Roi[] roisOfCurrentImage) {
    boolean isThere = false;
    for (Roi roi : roisOfCurrentImage) {
      final BigDecimal x = BigDecimal.valueOf(roi.getRotationCenter().xpoints[0]).round(new MathContext(5));
      final BigDecimal y = BigDecimal.valueOf(roi.getRotationCenter().ypoints[0]).round(new MathContext(5));
      final Pair<BigDecimal, BigDecimal> rotationCenterPoint = new Pair<>(x, y);
      if (point.getX().equals(rotationCenterPoint.getX()) && point.getY().equals(rotationCenterPoint.getY())) {
        isThere = true;
        break;
      }
    }
    return isThere;
  }
  
  private void warnUserIfNumberOfCornersIsNotTheSame(BufferedImage chosenImage, BufferedImage image) {
    int lengthCornersChosenImage = chosenImage.getManager().getRoisAsArray().length;
    int lengthCornersCurrentImage = image.getManager().getRoisAsArray().length;
    if (lengthCornersChosenImage != lengthCornersCurrentImage) {
      JOptionPane.showMessageDialog(null, TOO_FEW_CORNER_POINTS, "Warning", JOptionPane.WARNING_MESSAGE);
    }
  }
  
  private double getImageRatio(BufferedImage image) {
    return (double) image.getWidth() / (double) image.getHeight();
  }
  
  private Pair<BigDecimal, BigDecimal> convertPointRatio(Pair<BigDecimal, BigDecimal> point, double previousRatio, double currentRatio) {
    BigDecimal x = point.getX().multiply(BigDecimal.valueOf(currentRatio)).divide(BigDecimal.valueOf(previousRatio), new MathContext(5));
    BigDecimal y = point.getY().multiply(BigDecimal.valueOf(currentRatio)).divide(BigDecimal.valueOf(previousRatio), new MathContext(5));
    return new Pair<>(x, y);
  }
  
  private void addFile(AddFileEvent dialogEvent) {
    String pathFile = dialogEvent.getFilePath();
    try {
      long memory = ImageFile.estimateMemoryUsage(pathFile);
      this.totalMemory += memory;
      if (this.getTotalMemory() >= Runtime.getRuntime().maxMemory()) {
        JOptionPane.showMessageDialog(null, INSUFFICIENT_MEMORY_MESSAGE, INSUFFICIENT_MEMORY_TITLE, JOptionPane.ERROR_MESSAGE);
      }
      this.getManager().addFile(pathFile);
      this.getManager().addFileToOriginalList(pathFile);
    } catch (UnknownFormatException e) {
      this.getLoadingDialog().hideDialog();
      JOptionPane.showMessageDialog(null, UNKNOWN_FORMAT_MESSAGE, UNKNOWN_FORMAT_TITLE, JOptionPane.ERROR_MESSAGE);
    } catch (Exception e) {
      IJ.showMessage(e.getMessage());
    }
    this.getMainDialog().setPrevImageButtonEnabled(this.getManager().hasPrevious());
    this.getMainDialog().setNextImageButtonEnabled(this.getManager().hasNext());
    this.getMainDialog().setTitle(MessageFormat.format(MAIN_DIALOG_TITLE_PATTERN, this.getManager().getCurrentIndex() + 1, this.getManager().getNImages()));
    this.refreshRoiGUI();
  }
  
  /*
   * New Feature
   * TODO: refactor codebase when you have time
   **/
  private void autoAlign(AutoAlignEvent event) {
    AbstractBuilder builder = new FREAKBuilder(this.getLoadingDialog(), this.getManager(), event, this);
    builder.getLoadingDialog().showDialog();
    Utilities.setTimeout(() -> {
      try {
        this.alignHandler(builder);
      } catch (Exception e) {
        e.printStackTrace();
        IJ.showMessage(e.getMessage());
      }
      builder.getLoadingDialog().hideDialog();
    }, 10);
  }
  
  private void align(AlignEvent event) {
    AbstractBuilder builder = new LeastSquareTransformationBuilder(this.getLoadingDialog(), this.getManager(), event, this);
    builder.getLoadingDialog().showDialog();
    Utilities.setTimeout(() -> {
      try {
        if (event.isKeepOriginal()) {
          this.alignHandler(builder);
        } else {
          builder.build();
        }
      } catch (Exception e) {
        IJ.showMessage(e.getMessage());
      }
      builder.getLoadingDialog().hideDialog();
    }, 10);
  }
  
  private void alignHandler(AbstractBuilder builder) {
    builder.setTempImages(this.tempImages);
    builder.init();
    builder.align();
    builder.build();
    this.alignDialog = builder.getAlignDialog();
    this.tempImages = builder.getTempImages();
  }
  
  private void handleDeselectedRoi(DeselectedRoiEvent dialogEvent) {
    Arrays.stream(this.getImage().getManager().getSelectedRoisAsArray()).forEach(roi -> roi.setStrokeColor(Color.CYAN));
    this.getImage().getManager().select(dialogEvent.getRoiIndex());
    this.getPreviewDialog().drawRois();
  }
  
  private void handleSelectedRoiFromOval(SelectedRoiFromOvalEvent dialogEvent) {
    this.getMainDialog().jListRois.setSelectedIndex(dialogEvent.getRoiIndex());
  }
  
  private void deselectAllRois() {
    Arrays.stream(this.getImage().getManager().getRoisAsArray()).forEach(roi -> roi.setStrokeColor(Color.CYAN));
  }
  
  private void handleSelectedRoi(SelectedRoiEvent dialogEvent) {
    this.deselectAllRois();
    this.getImage().getManager().select(dialogEvent.getRoiIndex());
    this.getImage().getRoi().setStrokeColor(Color.yellow);
    this.drawAfterHandlingRois();
  }
  
  private void handleSelectedRois(SelectedRoisEvent dialogEvent) {
    this.deselectAllRois();
    final int[] rois = dialogEvent.getSelectedIndices();
    final Roi[] roiArrays = this.getImage().getManager().getRoisAsArray();
    IntStream.range(0, roiArrays.length).forEach(finalIndex -> {
      final boolean isYellow = Arrays.stream(rois).anyMatch(roiIndex -> finalIndex == roiIndex);
      final Roi roi = roiArrays[finalIndex];
      if (isYellow) {
        this.getImage().getManager().select(finalIndex);
        roi.setStrokeColor(Color.YELLOW);
      }
    });
    this.drawAfterHandlingRois();
  }
  
  private void drawAfterHandlingRois() {
    this.getImage().updateAndDraw();
    if (this.getPreviewDialog() != null && this.getPreviewDialog().isVisible()) {
      this.getPreviewDialog().drawRois();
    }
  }
  
  private void addRoi(AddRoiEvent dialogEvent) {
    Prefs.useNamesAsLabels = true;
    Prefs.noPointLabels = false;
    final int roiSize = (int) (Math.max(Toolkit.getDefaultToolkit().getScreenSize().width, this.getImage().getWidth()) * 0.03);
    final OvalRoi outer = new OvalRoi(dialogEvent.getClickCoordinates().getX().doubleValue() - ((double) roiSize / 2), dialogEvent.getClickCoordinates().getY().doubleValue() - ((double) roiSize / 2), roiSize, roiSize);
    // get roughly the 0,25% of the width of the image as stroke width of th rois added.
    // If the resultant value is too small, set it as the minimum value
    final int strokeWidth = Math.max((int) (this.getImage().getWidth() * 0.0025), 3);
    outer.setStrokeWidth(strokeWidth);
    outer.setImage(this.image);
    outer.setStrokeColor(Color.CYAN);
    final Overlay over = new Overlay();
    over.drawLabels(false);
    over.drawNames(true);
    over.drawBackgrounds(false);
    over.setLabelFontSize(Math.round(strokeWidth * 5f), "scale");
    over.setLabelColor(Color.CYAN);
    over.setStrokeWidth((double) strokeWidth);
    over.setStrokeColor(Color.CYAN);
    Arrays.stream(this.image.getManager().getRoisAsArray()).forEach(over::add);
    over.add(outer);
    this.getImage().getManager().setOverlay(over);
    this.getOriginalImage().getManager().setOverlay(over);
    this.refreshRoiGUI();
  }
  
  private void deleteRoi(DeleteRoiEvent dialogEvent) {
    this.getImage().getManager().select(dialogEvent.getRoiIndex());
    this.getImage().getManager().runCommand(DELETE_COMMAND);
    this.refreshRoiGUI();
  }
  
  private void deleteRois(DeleteRoisEvent dialogEvent) {
    int[] rois = dialogEvent.getRois();
    IntStream.range(0, rois.length).map(i -> rois[rois.length - 1 - i]).forEachOrdered(roi -> {
      this.getImage().getManager().select(roi);
      this.getImage().getManager().runCommand(DELETE_COMMAND);
      this.refreshRoiGUI();
    });
  }
  
  private void getChangeImageThread(ChangeImageEvent dialogEvent) {
    if (this.getImage() != null) {
      this.getImage().removeMouseListeners();
    }
    boolean isNext = dialogEvent.getChangeDirection() == ChangeImageEvent.ChangeDirection.NEXT && !this.getManager().hasNext();
    boolean isPrevious = dialogEvent.getChangeDirection() == ChangeImageEvent.ChangeDirection.PREV && !this.getManager().hasPrevious();
    if (isNext || isPrevious) {
      this.getLoadingDialog().hideDialog();
    }
    this.image = dialogEvent.getChangeDirection() == ChangeImageEvent.ChangeDirection.NEXT ? this.getManager().next() : this.getManager().previous();
    this.originalImage = this.getManager().getOriginal(this.getManager().getCurrentIndex(), false);
    if (this.getImage() != null) {
      this.getMainDialog().changeImage(this.getImage());
      this.getMainDialog().setPrevImageButtonEnabled(this.getManager().hasPrevious());
      this.getMainDialog().setNextImageButtonEnabled(this.getManager().hasNext());
      this.getMainDialog().setTitle(MessageFormat.format(MAIN_DIALOG_TITLE_PATTERN, this.getManager().getCurrentIndex() + 1, this.getManager().getNImages()));
      this.getImage().buildMouseListener();
      this.getLoadingDialog().hideDialog();
      this.refreshRoiGUI();
      this.getLoadingDialog().showDialog();
      this.getLoadingDialog().hideDialog();
    }
  }
  
  private void previewImage(PreviewImageEvent dialogEvent) {
    new Thread(() -> {
      if (!dialogEvent.getValue()) {
        this.getPreviewDialog().close();
        return;
      }
      try {
        this.getLoadingDialog().showDialog();
        this.previewDialog = new PreviewDialog(this.getManager().get(this.getManager().getCurrentIndex()), this, this.getManager().getCurrentIndex(), this.getManager().getNImages(), "Preview Image " + (this.getManager().getCurrentIndex() + 1) + "/" + this.getManager().getNImages());
      } catch (Exception e) {
        IJ.showMessage(e.getMessage());
      }
      this.getLoadingDialog().hideDialog();
      this.getPreviewDialog().pack();
      this.getPreviewDialog().setVisible(true);
      this.getPreviewDialog().drawRois();
    }).start();
  }
  
  @Override
  public void onPreviewDialogEvent(IPreviewDialogEvent dialogEvent) {
    if (dialogEvent instanceof ds4h.dialog.preview.event.ChangeImageEvent) {
      ds4h.dialog.preview.event.ChangeImageEvent event = (ds4h.dialog.preview.event.ChangeImageEvent) dialogEvent;
      new Thread(() -> {
        WindowManager.setCurrentWindow(this.getImage().getWindow());
        BufferedImage previewImage = this.getManager().get(event.getIndex());
        this.getPreviewDialog().changeImage(previewImage, "Preview Image " + (event.getIndex() + 1) + "/" + this.getManager().getNImages());
        this.getLoadingDialog().hideDialog();
      }).start();
      this.getLoadingDialog().showDialog();
    }
    
    if (dialogEvent instanceof CloseDialogEvent) {
      this.getMainDialog().setPreviewWindowCheckBox(false);
    }
  }
  
  @Override
  public void onAlignDialogEventListener(IAlignDialogEvent dialogEvent) {
    if (dialogEvent instanceof SaveEvent) {
      SaveDialog saveDialog = new SaveDialog("Save as", "aligned", TIFF_EXT);
      if (saveDialog.getFileName() == null) {
        this.getLoadingDialog().hideDialog();
        return;
      }
      String path = saveDialog.getDirectory() + saveDialog.getFileName();
      this.getLoadingDialog().showDialog();
      new FileSaver(this.getAlignDialog().getImagePlus()).saveAsTiff(path);
      this.getLoadingDialog().hideDialog();
      JOptionPane.showMessageDialog(null, IMAGE_SAVED_MESSAGE, "Save complete", JOptionPane.INFORMATION_MESSAGE);
      this.alignedImageSaved = true;
    }
    
    if (dialogEvent instanceof ReuseImageEvent) {
      this.disposeAll();
      this.initialize(Collections.singletonList(this.getTempImages().get(this.getTempImages().size() - 1)));
    }
    
    if (dialogEvent instanceof ds4h.dialog.align.event.ExitEvent) {
      if (!this.alignedImageSaved) {
        String[] buttons = {"Yes", "No"};
        int answer = JOptionPane.showOptionDialog(null, ALIGNED_IMAGE_NOT_SAVED_MESSAGE, CAREFUL_NOW_TITLE, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, buttons, buttons[1]);
        if (answer == 1) return;
      }
      this.getAlignDialog().setVisible(false);
      this.getAlignDialog().dispose();
    }
  }
  
  @Override
  public void onRemoveDialogEvent(IRemoveDialogEvent removeEvent) {
    if (removeEvent instanceof ExitEvent) {
      this.getRemoveImageDialog().setVisible(false);
      this.getRemoveImageDialog().dispose();
    }
    if (removeEvent instanceof ds4h.dialog.remove.event.RemoveImageEvent) {
      int imageFileIndex = ((ds4h.dialog.remove.event.RemoveImageEvent) removeEvent).getImageFileIndex();
      // only an image is available: if user remove this image we need to ask him to choose another one!
      if (this.manager.getImageFiles().size() == 1) {
        String[] buttons = {"Yes", "No"};
        int answer = JOptionPane.showOptionDialog(null, DELETE_ALL_IMAGES, CAREFUL_NOW_TITLE, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, buttons, buttons[1]);
        if (answer == 0) {
          String pathFile = this.promptForFile();
          if (pathFile.equals(NULL_PATH)) {
            this.disposeAll();
            return;
          }
          this.disposeAll();
          this.initialize(Collections.singletonList(pathFile));
        }
      } else {
        // remove the image selected
        this.getRemoveImageDialog().removeImageFile(imageFileIndex);
        this.getManager().removeImageFile(imageFileIndex);
        this.image = this.getManager().get(this.getManager().getCurrentIndex());
        this.originalImage = this.getManager().get(this.getManager().getCurrentIndex());
        this.getMainDialog().changeImage(this.image);
        this.getMainDialog().setPrevImageButtonEnabled(this.getManager().hasPrevious());
        this.getMainDialog().setNextImageButtonEnabled(this.getManager().hasNext());
        this.getMainDialog().setTitle(MessageFormat.format(MAIN_DIALOG_TITLE_PATTERN, this.getManager().getCurrentIndex() + 1, this.getManager().getNImages()));
        this.refreshRoiGUI();
      }
    }
  }
  
  /**
   * Refresh all the Roi-based guis in the MainDialog
   */
  private void refreshRoiGUI() {
    this.getMainDialog().drawRois(this.getImage().getManager());
    if (this.getPreviewDialog() != null && this.getPreviewDialog().isVisible()) {
      this.getPreviewDialog().drawRois();
    }
    // Get the number of rois added in each this.getImage(). If they are all the same (and at least one is added), we can enable the "align" functionality
    List<Integer> roisNumber = this.getManager().getRoiManagers().stream().map(roiManager -> roiManager.getRoisAsArray().length).collect(Collectors.toList());
    boolean alignButtonEnabled = roisNumber.get(0) >= LeastSquareImageTransformation.MINIMUM_ROI_NUMBER && this.getManager().getNImages() > 1 && roisNumber.stream().distinct().count() == 1;
    // check if: the number of images is more than 1, ALL the images has the same number of rois added and the ROI numbers are more than 3
    this.getMainDialog().setAlignButtonEnabled(alignButtonEnabled);
    boolean copyCornersEnabled = this.getManager().getRoiManagers().stream().filter(roiManager -> roiManager.getRoisAsArray().length != 0).map(roiManager -> this.getManager().getRoiManagers().indexOf(roiManager)).anyMatch(index -> index != this.getManager().getCurrentIndex());
    this.getMainDialog().setCopyCornersEnabled(copyCornersEnabled);
  }
  
  /**
   * Initialize the plugin opening the file specified in the mandatory param
   */
  public void initialize(List<String> filePaths) {
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
      this.getLoadingDialog().hideDialog();
      if (e instanceof OutOfMemoryError) {
        this.getLoadingDialog().hideDialog();
        JOptionPane.showMessageDialog(null, INSUFFICIENT_MEMORY_MESSAGE, INSUFFICIENT_MEMORY_TITLE, JOptionPane.ERROR_MESSAGE);
        System.exit(0);
      }
    });
    this.aboutDialog = new AboutDialog();
    this.loadingDialog = new LoadingDialog();
    this.getLoadingDialog().showDialog();
    this.alignedImageSaved = false;
    try {
      for (String filePath : filePaths) {
        this.showIfMemoryIsInsufficient(filePath);
      }
      this.manager = new ImagesManager(filePaths);
      this.image = this.getManager().next();
      this.originalImage = this.getManager().getOriginal(this.getManager().getCurrentIndex(), false);
      this.mainDialog = new MainDialog(this.image, this);
      this.getMainDialog().setPrevImageButtonEnabled(this.getManager().hasPrevious());
      this.getMainDialog().setNextImageButtonEnabled(this.getManager().hasNext());
      this.getMainDialog().setTitle(MessageFormat.format(MAIN_DIALOG_TITLE_PATTERN, this.getManager().getCurrentIndex() + 1, this.getManager().getNImages()));
      this.getMainDialog().pack();
      this.getMainDialog().setVisible(true);
      this.getLoadingDialog().hideDialog();
      if (this.getImage().isReduced())
        JOptionPane.showMessageDialog(null, IMAGES_SCALED_MESSAGE, "Info", JOptionPane.INFORMATION_MESSAGE);
      if (this.getManager().getNImages() == 1)
        JOptionPane.showMessageDialog(null, SINGLE_IMAGE_MESSAGE, "Warning", JOptionPane.WARNING_MESSAGE);
    } catch (ImagesManager.ImageOversizeException e) {
      JOptionPane.showMessageDialog(null, IMAGES_OVERSIZE_MESSAGE);
    } catch (UnknownFormatException | EnumException e) {
      JOptionPane.showMessageDialog(null, UNKNOWN_FORMAT_MESSAGE, UNKNOWN_FORMAT_TITLE, JOptionPane.ERROR_MESSAGE);
    } catch (Exception e) {
      IJ.showMessage(e.getMessage());
    } finally {
      this.getLoadingDialog().hideDialog();
    }
  }
  
  private void showIfMemoryIsInsufficient(String pathFile) throws IOException {
    long memory = ImageFile.estimateMemoryUsage(pathFile);
    this.totalMemory += memory;
    if (this.getTotalMemory() >= Runtime.getRuntime().maxMemory()) {
      JOptionPane.showMessageDialog(null, INSUFFICIENT_MEMORY_MESSAGE, INSUFFICIENT_MEMORY_TITLE, JOptionPane.ERROR_MESSAGE);
      this.run();
    }
  }
  
  private String promptForFile() {
    OpenDialog od = new OpenDialog("Select an image");
    String dir = od.getDirectory();
    String name = od.getFileName();
    return (dir + name);
  }
  
  /**
   * Dispose all the opened workload objects.
   */
  private void disposeAll() {
    this.getMainDialog().dispose();
    this.getLoadingDialog().hideDialog();
    this.getLoadingDialog().dispose();
    if (this.getPreviewDialog() != null) this.getPreviewDialog().dispose();
    if (this.getAlignDialog() != null) this.getAlignDialog().dispose();
    if (this.getRemoveImageDialog() != null) this.getRemoveImageDialog().dispose();
    this.getManager().dispose();
    this.totalMemory = 0;
  }
  
  
  public void run() {
    List<String> filePaths = new ArrayList<>(FileService.promptForFiles());
    filePaths.removeIf(filePath -> filePath.equals(NULL_PATH));
    this.initialize(filePaths);
  }
  
  private ImagesManager getManager() {
    return this.manager;
  }
  
  private LoadingDialog getLoadingDialog() {
    return this.loadingDialog;
  }
  
  private PreviewDialog getPreviewDialog() {
    return this.previewDialog;
  }
  
  private RemoveImageDialog getRemoveImageDialog() {
    return this.removeImageDialog;
  }
  
  private AlignDialog getAlignDialog() {
    return this.alignDialog;
  }
  
  private MainDialog getMainDialog() {
    return this.mainDialog;
  }
  
  private BufferedImage getImage() {
    return this.image;
  }
  
  private BufferedImage getOriginalImage() {
    return this.originalImage;
  }
  
  private long getTotalMemory() {
    return this.totalMemory;
  }
  
  public List<String> getTempImages() {
    return this.tempImages;
  }
  
  /**
   * This is a workaround I didn't want to use, but I don't have much time left
   * If you are the developer that has to refactor all this
   * (Refactor is a bit of a stretch, I know)
   * Take into account also this.
   * What was done here can't be called MVC at all, so you must do it.
   * Note: In a good MVC you can change the View part with what you can see fit,
   * without having to adapt the Model and Controller parts
   *
   * @param event - Event that is launched by the observable
   */
  @Override
  public void propertyChange(PropertyChangeEvent event) {
    if (event.getPropertyName().equals("updatedImage")) {
      String oldPath = (String) event.getOldValue();
      String newPath = (String) event.getNewValue();
      this.getTempImages().remove(oldPath);
      this.getTempImages().add(newPath);
    }
  }
}

