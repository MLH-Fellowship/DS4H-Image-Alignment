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
import ds4h.builder.AlignBuilder;
import ds4h.builder.BriefBuilder;
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
import ds4h.dialog.preview.PreviewDialog;
import ds4h.dialog.remove.OnRemoveDialogEventListener;
import ds4h.dialog.remove.RemoveImageDialog;
import ds4h.dialog.remove.event.IRemoveDialogEvent;
import ds4h.dialog.remove.event.RemoveImageEvent;
import ds4h.image.model.manager.ImageFile;
import ds4h.image.model.manager.ImagesEditor;
import ds4h.image.model.manager.slide.SlideImage;
import ds4h.image.model.project.Project;
import ds4h.image.model.project.ProjectImage;
import ds4h.image.model.project.ProjectImageRoi;
import ds4h.services.FileService;
import ds4h.services.ProjectService;
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
import ij.io.SaveDialog;
import ij.plugin.frame.RoiManager;
import loci.common.enumeration.EnumException;
import loci.formats.UnknownFormatException;
import org.opencv.core.Mat;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.MessageFormat;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ImageAlignment implements OnMainDialogEventListener, OnAlignDialogEventListener, OnRemoveDialogEventListener, PropertyChangeListener {
    private static final String IMAGES_SCALED_MESSAGE = "Image size too large: image has been scaled for compatibility.";
    private static final String SINGLE_IMAGE_MESSAGE = "Only one image detected in the stack: align operation will be unavailable.";
    private static final String IMAGES_OVERSIZE_MESSAGE = "Cannot open the selected image: image exceed supported dimensions.";
    private static final String SAVE_PROJECT_TITLE_SUCCESS = "The project was successfully saved";
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
        Loader loader = new Loader();
        loader.load();
    }

    private List<String> tempImages = new ArrayList<>();
    private ImagesEditor editor;
    private MainDialog mainDialog;
    private PreviewDialog previewDialog;
    private AlignDialog alignDialog;
    private LoadingDialog loadingDialog;
    private AboutDialog aboutDialog;
    private RemoveImageDialog removeImageDialog;
    private long totalMemory = 0;

    @Override
    public void onMainDialogEvent(MainDialogEvent dialogEvent) {
        if (this.getEditor().getCurrentImage() != null) {
            WindowManager.setCurrentWindow(this.getEditor().getCurrentImage().getWindow());
        }
        /**
         * TODO: A solution to this big switch case could be simply Polymorphism!
         * TODO: It's an easy fix it but as always I'm really sorry but I don't have time to do that
         */
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
            case "OpenAboutEvent":
                this.aboutDialog.setVisible(true);
                break;
            case "LoadProjectEvent":
                this.loadProject();
                break;
            case "SaveProjectEvent":
                this.saveProject();
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

    private void loadProject() {
        // clear old data
        this.disposeAll();
        final Project project = ProjectService.load();
        if (project == null) {
            return;
        }
        // load new one
        this.initialize(project.getProjectImages().stream().map(ProjectImage::getFilePath).collect(Collectors.toList()));
        // set rois for roiManagers
        this.applyCorners(project);
    }

    private void saveProject() {
        try {
            this.getEditor().loadImagesWholeSlides();
            int counterBeforeThisId = 0;
            final Project project = new Project();
            final List<ProjectImage> projectImages = new ArrayList<>();
            final List<ImageFile> imageFiles = this.getEditor().getImageFiles();
            for (final ImageFile imageFile : imageFiles) {
                final ProjectImage projectImage = new ProjectImage(imageFile.getImagesCounter(), imageFile.getPathFile());
                final List<ProjectImageRoi> projectImageRois = new ArrayList<>();
                for (int imageFileIndex = 0; imageFileIndex < imageFile.getImagesCounter(); imageFileIndex++) {
                    projectImage.setId(counterBeforeThisId);
                    final RoiManager roiManager = imageFile.getImage(imageFileIndex, true).getManager();
                    final Roi[] roisAsArray = roiManager.getRoisAsArray();
                    for (int roiIndex = 0; roiIndex < roisAsArray.length; roiIndex++) {
                        Roi roi = roisAsArray[roiIndex];
                        final BigDecimal x = BigDecimal.valueOf(roi.getRotationCenter().xpoints[0]);
                        final BigDecimal y = BigDecimal.valueOf(roi.getRotationCenter().ypoints[0]);
                        final ProjectImageRoi projectImageRoi = new ProjectImageRoi();
                        projectImageRoi.setPoint(new Pair<>(x, y));
                        projectImageRoi.setId(roiIndex);
                        projectImageRoi.setImageIndex(imageFileIndex);
                        projectImageRois.add(projectImageRoi);
                    }
                }
                counterBeforeThisId += imageFile.getImagesCounter();
                projectImage.setProjectImageRois(projectImageRois);
                projectImages.add(projectImage);
            }
            project.setProjectImages(projectImages);
            final String outputPath = FileService.chooseDirectoryViaIJ();
            if (outputPath.isEmpty()) {
                return;
            }
            ProjectService.save(project, this.getEditor().getCurrentImage().getFilePath().substring(0, this.getEditor().getCurrentImage().getFilePath().lastIndexOf(File.separator)), outputPath);
            JOptionPane.showMessageDialog(null, "The project was saved here: " + outputPath, SAVE_PROJECT_TITLE_SUCCESS, JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) {
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
    }

    private void handleMovedRoi() {
        this.getMainDialog().refreshROIList(this.getEditor().getCurrentImage().getManager());
        if (this.getPreviewDialog() != null) this.getPreviewDialog().drawRois();
    }

    private void removeImage() {
        this.getLoadingDialog().showDialog();
        Utilities.setTimeout(() -> {
            this.removeImageDialog = new RemoveImageDialog(this.getEditor().getImageFiles(), this);
            this.getRemoveImageDialog().setVisible(true);
            this.getLoadingDialog().hideDialog();
            this.getLoadingDialog().requestFocus();
        }, 20);
    }

    public void applyCorners(Project project) {
        project.getProjectImages().stream().sorted(Comparator.comparingInt(ProjectImage::getId)).forEachOrdered(projectImage -> projectImage.getProjectImageRois().forEach(projectImageRoi -> {
            final SlideImage slideImageTemp = this.getEditor().getSlideImage(projectImage.getId() + projectImageRoi.getImageIndex());
            final SlideImage originalSlideImageTemp = this.getEditor().getOriginalSlideImage(projectImage.getId() + projectImageRoi.getImageIndex());
            addRoiInLoadingProject(projectImageRoi, slideImageTemp, originalSlideImageTemp);
        }));
    }

    private void addRoiInLoadingProject(ProjectImageRoi projectImageRoi, SlideImage slideImageTemp, SlideImage originalSlideImageTemp) {
        // Code duplication ( like everywhere in the project, sorry I don't have time )
        final int roiSize = (int) (Math.max(Toolkit.getDefaultToolkit().getScreenSize().width, this.getEditor().getCurrentImage().getWidth()) * 0.03);
        final Pair<BigDecimal, BigDecimal> point = projectImageRoi.getPoint();
        final OvalRoi outer = new OvalRoi(point.getFirst().doubleValue() - ((double) roiSize / 2), point.getSecond().doubleValue() - ((double) roiSize / 2), roiSize, roiSize);
        final Overlay over = createOverlay(slideImageTemp.getWidth());
        final int strokeWidth = Math.max((int) (slideImageTemp.getWidth() * 0.0025), 3);
        Arrays.stream(slideImageTemp.getManager().getRoisAsArray()).forEach(over::add);
        over.add(outer);
        outer.setStrokeColor(Color.CYAN);
        outer.setStrokeWidth(strokeWidth);
        outer.setImage(slideImageTemp);
        slideImageTemp.getManager().addRoi(outer);
        slideImageTemp.getManager().setOverlay(over);
        originalSlideImageTemp.getManager().setOverlay(over);
        this.refreshRoiGUI();
    }

    /*private int getImageIndexByFilePath(Pair<String, Integer> pair) {
        return IntStream.range(0, this.getManager().getImageFiles().size()).filter(i -> {
            final String pathFileSub = pair.getFirst().substring(pair.getFirst().lastIndexOf(File.separator) + 1);
            return this.getManager().getImageFiles().get(i).getPathFile().endsWith(pathFileSub);
        }).findFirst().orElse(-1);
    }*/

    private void copyCorners() {
        // get the indexes of all roi managers with at least a roi added
        List<Integer> imageIndexes;
        // remove the index of the current image, if present.
        final List<Integer> list = new ArrayList<>();
        final SlideImage currentSlideImage = this.getMainDialog().getCurrentImage();
        final Roi[] roisOfCurrentImage = currentSlideImage.getManager().getRoisAsArray();
        final List<ImageFile> images = this.getEditor().getImageFiles();
        final List<RoiManager> roiManagers = images.stream().flatMap(imageFile -> imageFile.getImages().stream()).map(SlideImage::getManager).collect(Collectors.toList());

        for (RoiManager roiManager : roiManagers) {
            if (roiManager.getRoisAsArray().length == 0) {
                continue;
            }
            int index = roiManagers.indexOf(roiManager);
            if (index != this.getEditor().getCurrentPosition()) {
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
            SlideImage chosenSlideImage = this.editor.getSlideImage(optionList.getSelectedIndex());
            RoiManager selectedManager = this.getEditor().getSlideImage(imageIndexes.get(optionList.getSelectedIndex())).getManager();
            Map<Integer, Pair<BigDecimal, BigDecimal>> roiPoints = this.handleRoiInNewImage(roisOfCurrentImage, selectedManager);
            // I'll definitely have to refactor all this duplicated code hanging around
            if (!roiPoints.isEmpty()) {
                this.handleRoiPointsThatWereNotAdded(chosenSlideImage, roiPoints);
            }
            this.refreshRoiGUI();
            this.getEditor().getCurrentImage().setCopyCornersMode(true);
        }
    }

    private Map<Integer, Pair<BigDecimal, BigDecimal>> handleRoiInNewImage(Roi[] roisOfCurrentImage, RoiManager selectedManager) {
        Map<Integer, Pair<BigDecimal, BigDecimal>> roiPoints = new HashMap<>();
        Roi[] rois = selectedManager.getRoisAsArray();
        IntStream.range(0, rois.length).forEach(index -> {
            final Roi roi = rois[index];
            final BigDecimal x = BigDecimal.valueOf(roi.getXBase() + (roi.getFloatWidth() / 2));
            final BigDecimal y = BigDecimal.valueOf(roi.getYBase() + (roi.getFloatHeight() / 2));
            Pair<BigDecimal, BigDecimal> point = new Pair<>(x, y);
            if (this.isAlreadyThere(point, roisOfCurrentImage)) {
                return;
            }
            if (point.getFirst().intValue() < this.getEditor().getCurrentImage().getWidth() && point.getSecond().intValue() < this.getEditor().getCurrentImage().getHeight()) {
                this.onMainDialogEvent(new AddRoiEvent(point));
                return;
            }
            roiPoints.put(index, point);
        });
        return roiPoints;
    }

    private void handleRoiPointsThatWereNotAdded(SlideImage chosenSlideImage, Map<Integer, Pair<BigDecimal, BigDecimal>> roiPoints) {
        String[] buttons = {"Yes", "No"};
        String message = "The corners out of bounds are deleted. Do you want to delete the same corners in the referenced image?";
        int answer = JOptionPane.showOptionDialog(null, message, CAREFUL_NOW_TITLE, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, buttons, buttons[1]);
        if (answer == 0) {
            int[] roisIndices = roiPoints.keySet().stream().mapToInt(value -> value).toArray();
            // Keep in mind that when you want to delete multiple rois, it should be done from in reverse order
            IntStream.range(0, roisIndices.length).map(i -> roisIndices[roisIndices.length - 1 - i]).forEachOrdered(index -> {
                chosenSlideImage.getManager().select(index);
                if (chosenSlideImage.getManager().isSelected(index)) {
                    chosenSlideImage.getManager().runCommand(DELETE_COMMAND);
                }
            });
        }
        this.warnUserIfNumberOfCornersIsNotTheSame(chosenSlideImage, getEditor().getCurrentImage());
    }

    private boolean isAlreadyThere(Pair<BigDecimal, BigDecimal> point, Roi[] roisOfCurrentImage) {
        boolean isThere = false;
        for (Roi roi : roisOfCurrentImage) {
            final BigDecimal x = BigDecimal.valueOf(roi.getRotationCenter().xpoints[0]).round(new MathContext(5));
            final BigDecimal y = BigDecimal.valueOf(roi.getRotationCenter().ypoints[0]).round(new MathContext(5));
            final Pair<BigDecimal, BigDecimal> rotationCenterPoint = new Pair<>(x, y);
            if (point.getFirst().equals(rotationCenterPoint.getFirst()) && point.getSecond().equals(rotationCenterPoint.getSecond())) {
                isThere = true;
                break;
            }
        }
        return isThere;
    }

    private void warnUserIfNumberOfCornersIsNotTheSame(SlideImage chosenSlideImage, SlideImage slideImage) {
        final int lengthCornersChosenImage = chosenSlideImage.getManager().getRoisAsArray().length;
        final int lengthCornersCurrentImage = slideImage.getManager().getRoisAsArray().length;
        if (lengthCornersChosenImage != lengthCornersCurrentImage) {
            JOptionPane.showMessageDialog(null, TOO_FEW_CORNER_POINTS, "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

   /*private double getImageRatio(BufferedImage image) {
        return (double) image.getWidth() / (double) image.getHeight();
    }

    private Pair<BigDecimal, BigDecimal> convertPointRatio(Pair<BigDecimal, BigDecimal> point, double previousRatio, double currentRatio) {
        BigDecimal x = point.getFirst().multiply(BigDecimal.valueOf(currentRatio)).divide(BigDecimal.valueOf(previousRatio), new MathContext(5));
        BigDecimal y = point.getSecond().multiply(BigDecimal.valueOf(currentRatio)).divide(BigDecimal.valueOf(previousRatio), new MathContext(5));
        return new Pair<>(x, y);
    }*/

    private void addFile(AddFileEvent dialogEvent) {
        String pathFile = dialogEvent.getFilePath();
        try {
            long memory = ImageFile.estimateMemoryUsage(pathFile);
            this.totalMemory += memory;
            if (this.getTotalMemory() >= Runtime.getRuntime().maxMemory()) {
                JOptionPane.showMessageDialog(null, INSUFFICIENT_MEMORY_MESSAGE, INSUFFICIENT_MEMORY_TITLE, JOptionPane.ERROR_MESSAGE);
            }
            this.getEditor().addFile(pathFile);
            this.getEditor().addFileToOriginalList(pathFile);
            if (this.getEditor().getAllImagesCounterSum() > 1) {
                this.getMainDialog().setAutoAlignButtonEnabled(true);
            }
        } catch (UnknownFormatException e) {
            this.getLoadingDialog().hideDialog();
            JOptionPane.showMessageDialog(null, UNKNOWN_FORMAT_MESSAGE, UNKNOWN_FORMAT_TITLE, JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            IJ.showMessage(e.getMessage());
        }
        this.getMainDialog().setPrevImageButtonEnabled(this.getEditor().hasPrevious());
        this.getMainDialog().setNextImageButtonEnabled(this.getEditor().hasNext());
        this.getMainDialog().setTitle(MessageFormat.format(MAIN_DIALOG_TITLE_PATTERN, this.getEditor().getCurrentPosition() + 1, this.getEditor().getAllImagesCounterSum()));
        this.refreshRoiGUI();
    }

    /*
     * New Feature
     * TODO: refactor codebase when you have time
     **/
    private void autoAlign(AutoAlignEvent event) {
        AbstractBuilder<Mat> builder = new BriefBuilder(this.getLoadingDialog(), this.getEditor(), event, this);
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
        AbstractBuilder<SlideImage> builder = new LeastSquareTransformationBuilder(this.getLoadingDialog(), this.getEditor(), event, this);
        builder.getLoadingDialog().showDialog();
        Utilities.setTimeout(() -> {
            try {
                this.alignHandler(builder);
            } catch (Exception e) {
                System.out.println(e.getMessage() + " " + Arrays.toString(e.getStackTrace()));
            }
            builder.getLoadingDialog().hideDialog();
        }, 10);
    }

    private void alignHandler(AlignBuilder builder) {
        builder.setTempImages(this.tempImages);
        builder.init();
        if (builder.check()) {
            if (builder.getEvent().isKeepOriginal()) {
                builder.alignKeepOriginal();
            } else {
                builder.align();
            }
            builder.build();
            this.alignDialog = builder.getAlignDialog();
            this.tempImages = builder.getTempImages();
            this.getMainDialog().setAutoAlignButtonEnabled(this.getEditor().getAllImagesCounterSum() > 1);
        }
    }

    private void handleDeselectedRoi(DeselectedRoiEvent dialogEvent) {
        Arrays.stream(this.getEditor().getCurrentImage().getManager().getSelectedRoisAsArray()).forEach(roi -> roi.setStrokeColor(Color.CYAN));
        this.getEditor().getCurrentImage().getManager().select(dialogEvent.getRoiIndex());
        this.getPreviewDialog().drawRois();
    }

    private void handleSelectedRoiFromOval(SelectedRoiFromOvalEvent dialogEvent) {
        this.getMainDialog().jListRois.setSelectedIndex(dialogEvent.getRoiIndex());
    }

    private void deselectAllRois() {
        Arrays.stream(this.getEditor().getCurrentImage().getManager().getRoisAsArray()).forEach(roi -> roi.setStrokeColor(Color.CYAN));
    }

    private void handleSelectedRoi(SelectedRoiEvent dialogEvent) {
        this.deselectAllRois();
        this.getEditor().getCurrentImage().getManager().select(dialogEvent.getRoiIndex());
        this.getEditor().getCurrentImage().getRoi().setStrokeColor(Color.yellow);
        this.drawAfterHandlingRois();
    }

    private void handleSelectedRois(SelectedRoisEvent dialogEvent) {
        this.deselectAllRois();
        final int[] rois = dialogEvent.getSelectedIndices();
        final Roi[] roiArrays = this.getEditor().getCurrentImage().getManager().getRoisAsArray();
        IntStream.range(0, roiArrays.length).forEach(finalIndex -> {
            final boolean isYellow = Arrays.stream(rois).anyMatch(roiIndex -> finalIndex == roiIndex);
            final Roi roi = roiArrays[finalIndex];
            if (isYellow) {
                this.getEditor().getCurrentImage().getManager().select(finalIndex);
                roi.setStrokeColor(Color.YELLOW);
            }
        });
        this.drawAfterHandlingRois();
    }

    private void drawAfterHandlingRois() {
        this.getEditor().getCurrentImage().updateAndDraw();
        if (this.getPreviewDialog() != null && this.getPreviewDialog().isVisible()) {
            this.getPreviewDialog().drawRois();
        }
    }

    private void addRoi(AddRoiEvent dialogEvent) {
        Prefs.useNamesAsLabels = true;
        Prefs.noPointLabels = false;
        final int roiSize = (int) (Math.max(Toolkit.getDefaultToolkit().getScreenSize().width, this.getEditor().getCurrentImage().getWidth()) * 0.03);
        final OvalRoi outer = new OvalRoi(dialogEvent.getClickCoordinates().getFirst().doubleValue() - ((double) roiSize / 2), dialogEvent.getClickCoordinates().getSecond().doubleValue() - ((double) roiSize / 2), roiSize, roiSize);
        // get roughly the 0,25% of the width of the image as stroke width of th rois added.
        // If the resultant value is too small, set it as the minimum value
        final int strokeWidth = Math.max((int) (this.getEditor().getCurrentImage().getWidth() * 0.0025), 3);
        outer.setStrokeWidth(strokeWidth);
        outer.setImage(this.getEditor().getCurrentImage());
        outer.setStrokeColor(Color.CYAN);
        final Overlay over = this.createOverlay(strokeWidth);
        Arrays.stream(this.getEditor().getCurrentImage().getManager().getRoisAsArray()).forEach(over::add);
        over.add(outer);
        this.getEditor().getCurrentImage().getManager().setOverlay(over);
        this.getEditor().getCurrentOriginalImage().getManager().setOverlay(over);
        this.refreshRoiGUI();
    }

    private Overlay createOverlay(int strokeWidth) {
        final Overlay over = new Overlay();
        over.drawLabels(false);
        over.drawNames(true);
        over.drawBackgrounds(false);
        over.setLabelFontSize(Math.round(strokeWidth * 5f), "scale");
        over.setLabelColor(Color.CYAN);
        over.setStrokeWidth((double) strokeWidth);
        over.setStrokeColor(Color.CYAN);
        return over;
    }

    private void deleteRoi(DeleteRoiEvent dialogEvent) {
        this.getEditor().getCurrentImage().getManager().select(dialogEvent.getRoiIndex());
        this.getEditor().getCurrentImage().getManager().runCommand(DELETE_COMMAND);
        this.getEditor().getCurrentOriginalImage().getManager().select(dialogEvent.getRoiIndex());
        this.getEditor().getCurrentOriginalImage().getManager().runCommand(DELETE_COMMAND);
        this.refreshRoiGUI();
    }

    private void deleteRois(DeleteRoisEvent dialogEvent) {
        int[] rois = dialogEvent.getRois();
        IntStream.range(0, rois.length).map(i -> rois[rois.length - 1 - i]).forEachOrdered(roi -> {
            this.getEditor().getCurrentImage().getManager().select(roi);
            this.getEditor().getCurrentImage().getManager().runCommand(DELETE_COMMAND);
            this.getEditor().getCurrentOriginalImage().getManager().select(roi);
            this.getEditor().getCurrentOriginalImage().getManager().runCommand(DELETE_COMMAND);
            this.refreshRoiGUI();
        });
    }

    private void getChangeImageThread(ChangeImageEvent dialogEvent) {
        if (this.getEditor().getCurrentImage() != null) {
            this.getMainDialog().removeMouseListener();
        }
        boolean isNext = dialogEvent.getChangeDirection() == ChangeImageEvent.ChangeDirection.NEXT && !this.getEditor().hasNext();
        boolean isPrevious = dialogEvent.getChangeDirection() == ChangeImageEvent.ChangeDirection.PREV && !this.getEditor().hasPrevious();
        if (isNext || isPrevious) {
            this.getLoadingDialog().hideDialog();
        }
        if (dialogEvent.getChangeDirection() == ChangeImageEvent.ChangeDirection.NEXT) {
            this.getEditor().next();
        } else {
            this.getEditor().previous();
        }

        if (this.getEditor().getCurrentImage() != null) {
            SwingUtilities.invokeLater(() -> {
                this.getMainDialog().changeImage(this.getEditor().getCurrentImage());
                this.getMainDialog().setPrevImageButtonEnabled(this.getEditor().hasPrevious());
                this.getMainDialog().setNextImageButtonEnabled(this.getEditor().hasNext());
                this.getMainDialog().setTitle(MessageFormat.format(MAIN_DIALOG_TITLE_PATTERN, this.getEditor().getCurrentPosition() + 1, this.getEditor().getAllImagesCounterSum()));
                this.getLoadingDialog().hideDialog();
                this.refreshRoiGUI();
                this.getLoadingDialog().showDialog();
                this.getLoadingDialog().hideDialog();
            });
        }
    }

    private void previewImage(PreviewImageEvent dialogEvent) {
            if (!dialogEvent.getValue()) {
                SwingUtilities.invokeLater(() -> {
                    this.getLoadingDialog().hideDialog();
                    this.getMainDialog().setImage(this.getEditor().getCurrentImage());
                    WindowManager.setCurrentWindow(this.getMainDialog());
                    WindowManager.getCurrentWindow().getCanvas().fitToWindow();
                    this.getMainDialog().setVisible(true);
                    this.getMainDialog().setPrevImageButtonEnabled(this.getEditor().hasPrevious());
                    this.getMainDialog().setNextImageButtonEnabled(this.getEditor().hasNext());
                    this.getMainDialog().setTitle(MessageFormat.format(MAIN_DIALOG_TITLE_PATTERN, this.getEditor().getCurrentPosition() + 1, this.getEditor().getAllImagesCounterSum()));
                });
                return;
            }
        SwingUtilities.invokeLater(() -> {
            try {
                this.getLoadingDialog().showDialog();
                ImagesEditor editorClone = new ImagesEditor(this.getEditor());
                this.getLoadingDialog().hideDialog();
                this.previewDialog = new PreviewDialog(editorClone, this.getMainDialog());
                WindowManager.setCurrentWindow(this.getPreviewDialog());
            } catch (Exception e) {
                IJ.showMessage(e.getMessage());
            }
            this.getPreviewDialog().pack();
            this.getPreviewDialog().setVisible(true);
            this.getPreviewDialog().drawRois();
        });
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
        }

        if (dialogEvent instanceof ReuseImageEvent) {
            this.disposeAll();
            this.initialize(Collections.singletonList(this.getTempImages().get(this.getTempImages().size() - 1)));
        }
    }

    @Override
    public void onRemoveDialogEvent(IRemoveDialogEvent removeEvent) {
        if (removeEvent instanceof RemoveImageEvent) {
            int imageFileIndex = ((RemoveImageEvent) removeEvent).getImageFileIndex();
            // only an image is available: if user remove this image we need to ask him to choose another one!
            if (this.editor.getImageFiles().size() == 1) {
                String[] buttons = {"Yes", "No"};
                int answer = JOptionPane.showOptionDialog(null, DELETE_ALL_IMAGES, CAREFUL_NOW_TITLE, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, buttons, buttons[1]);
                if (answer == 0) {
                    this.disposeAll();
                    this.run();
                    return;
                }
            } else {
                Utilities.setTimeout(() -> {
                    // remove the image selected
                    this.getRemoveImageDialog().removeImageFile(imageFileIndex);
                    this.getEditor().removeImageFile(imageFileIndex);
                    this.getEditor().removeOriginalImageFile(imageFileIndex);
                    this.getEditor().next();
                }, 10);
            }
            Utilities.setTimeout(() -> {
                this.getMainDialog().changeImage(this.getEditor().getCurrentImage());
                int finalIndex = this.getEditor().getCurrentPosition();
                if (finalIndex < 0) {
                    finalIndex = 0;
                }
                if (this.getEditor().getAllImagesCounterSum() == 1) {
                    this.getMainDialog().setAutoAlignButtonEnabled(false);
                }
                finalIndex = finalIndex + 1;
                if (this.getEditor().getAllImagesCounterSum() < finalIndex && this.getEditor().getCurrentPosition() == this.getEditor().getAllImagesCounterSum()) {
                    finalIndex = this.getEditor().getAllImagesCounterSum();
                }
                this.getMainDialog().setPrevImageButtonEnabled(finalIndex > 1 && this.getEditor().getCurrentPosition() != 0);
                this.getMainDialog().setNextImageButtonEnabled(getEditor().hasNext());
                this.getMainDialog().setTitle(MessageFormat.format(MAIN_DIALOG_TITLE_PATTERN, finalIndex, this.getEditor().getAllImagesCounterSum()));
                this.refreshRoiGUI();
                this.getRemoveImageDialog().dispose();
            }, 500);
        }
    }

    /**
     * Refresh all the Roi-based guis in the MainDialog
     */
    private void refreshRoiGUI() {
        this.getMainDialog().drawRois(this.getEditor().getCurrentImage().getManager());
        if (this.getPreviewDialog() != null && this.getPreviewDialog().isVisible()) {
            this.getPreviewDialog().drawRois();
        }
        List<RoiManager> roiManagers = this.getEditor().getImageFiles().stream().flatMap(imageFile -> imageFile.getImages().stream()).map(SlideImage::getManager).collect(Collectors.toList());
        // Get the number of rois added in each this.getEditor().getCurrentImage()(). If they are all the same (and at least one is added), we can enable the "align" functionality
        List<Integer> roisNumber = roiManagers.stream().map(roiManager -> roiManager.getRoisAsArray().length).collect(Collectors.toList());
        boolean alignButtonEnabled = roisNumber.get(0) >= LeastSquareImageTransformation.MINIMUM_ROI_NUMBER && this.getEditor().getAllImagesCounterSum() > 1 && roisNumber.stream().distinct().count() == 1;
        // check if: the number of images is more than 1, ALL the images has the same number of rois added and the ROI numbers are more than 3
        this.getMainDialog().setAlignButtonEnabled(alignButtonEnabled);
        boolean copyCornersEnabled = roiManagers.stream().filter(roiManager -> roiManager.getRoisAsArray().length != 0).anyMatch(roiManager -> roiManager != this.getEditor().getCurrentImage().getManager());
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
        try {
            for (String filePath : filePaths) {
                this.showIfMemoryIsInsufficient(filePath);
            }
            this.editor = new ImagesEditor(filePaths);
            this.editor.addPropertyChangeListener(this);
            this.getEditor().next();
            this.mainDialog = new MainDialog(this.getEditor().getCurrentImage(), this);
            this.getMainDialog().setPrevImageButtonEnabled(this.getEditor().hasPrevious());
            this.getMainDialog().setNextImageButtonEnabled(this.getEditor().hasNext());
            this.getMainDialog().setTitle(MessageFormat.format(MAIN_DIALOG_TITLE_PATTERN, this.getEditor().getCurrentPosition() + 1, this.getEditor().getAllImagesCounterSum()));
            this.getMainDialog().setAutoAlignButtonEnabled(this.getEditor().getAllImagesCounterSum() > 1);
            this.getLoadingDialog().hideDialog();
            if (this.getEditor().getCurrentImage().isReduced())
                JOptionPane.showMessageDialog(null, IMAGES_SCALED_MESSAGE, "Info", JOptionPane.INFORMATION_MESSAGE);
            if (this.getEditor().getAllImagesCounterSum() == 1)
                JOptionPane.showMessageDialog(null, SINGLE_IMAGE_MESSAGE, "Warning", JOptionPane.WARNING_MESSAGE);
        } catch (ImagesEditor.ImageOversizeException e) {
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
        this.getEditor().dispose();
        this.totalMemory = 0;
    }


    public void run() {
        List<String> filePaths = new ArrayList<>(FileService.promptForFiles());
        filePaths.removeIf(filePath -> filePath.equals(NULL_PATH));
        if (filePaths.isEmpty()) {
            return;
        }
        this.initialize(filePaths);
    }

    private ImagesEditor getEditor() {
        return this.editor;
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
