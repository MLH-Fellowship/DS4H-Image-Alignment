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
import ds4h.dialog.main.event.AutoAlignEvent;
import ds4h.image.manager.ImagesManager;
import ds4h.image.model.ImageFile;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Point;
import org.opencv.core.*;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.imgproc.Imgproc;
import org.opencv.xfeatures2d.FREAK;
import org.opencv.xfeatures2d.StarDetector;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opencv.core.CvType.CV_8U;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.imgcodecs.Imgcodecs.IMREAD_GRAYSCALE;
import static org.opencv.imgcodecs.Imgcodecs.imread;

/**
 * This plugin is made as a non-profit utility
 * It uses XFeatures2d features like FREAK and StartDetector
 * Given that is used in the medical research field and no kind of profit is made from this
 * There isn't any break of the copyright law.
 * For any issues the owners of the algorithms see fit, post an issue here :
 * https://github.com/Edodums/DS4H-Image-Alignment/issues
 */
public class FREAKBuilder extends AbstractBuilder {
  private final List<Mat> images = new ArrayList<>();
  private final Map<Long, String> pathMap = new HashMap<>();
  
  public FREAKBuilder(LoadingDialog loadingDialog, ImagesManager manager, AutoAlignEvent event, OnAlignDialogEventListener listener) {
    super(loadingDialog, listener, manager, event);
  }
  
  @Override
  public void init() {
    this.importImages();
    this.setMaximumSize(new Dimension(this.getSourceImage().width(), this.getSourceImage().height()));
    this.setVirtualStack();
  }
  
  @Override
  public void align() {
    try {
      this.newProcessHandler(this.getSourceImage());
      for (int index = 1; index < images.size(); index++) {
        // two images
        final Mat firstImage = this.getSourceImage();
        final Mat secondImage = images.get(index);
        final MatOfKeyPoint firstKeyPoints = this.getKeypoint(firstImage);
        final MatOfKeyPoint secondKeyPoints = this.getKeypoint(secondImage);
        final Mat firstDescriptor = this.getDescriptor(firstImage, firstKeyPoints);
        final Mat secondDescriptor = this.getDescriptor(secondImage, secondKeyPoints);
        // match
        final DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        // convert to float to use flann ( Freak is a binary descriptor )
        if (firstDescriptor.type() != CV_8U) {
          firstDescriptor.convertTo(firstDescriptor, CV_8U);
        }
        if (secondDescriptor.type() != CV_8U) {
          secondDescriptor.convertTo(secondDescriptor, CV_8U);
        }
        // match descriptors and filter to avoid false positives
        List<MatOfDMatch> knnMatches = new ArrayList<>();
        try {
          matcher.knnMatch(firstDescriptor, secondDescriptor, knnMatches, 2);
        } catch (Exception e) {
          IJ.showMessage("Check all your images, one of them seems to have not valuable matches for our algorithm");
        }
        List<DMatch> goodMatches = getGoodMatches(knnMatches);
        // Below four matches the images couldn't be related
        if (goodMatches.size() > 4) {
          final MatOfPoint2f points = new MatOfPoint2f(this.getPointsArray(firstImage));
          Mat dest = new Mat();
          // Check directly the Javadoc, to learn more
          Core.perspectiveTransform(points, dest, this.getHomography(goodMatches, firstKeyPoints.toList(), secondKeyPoints.toList()));
          final Mat perspectiveM = Imgproc.getPerspectiveTransform(points, dest);
          Mat warpedImage = new Mat();
          // Literally takes the secondImage, the perspective transformation matrix, the size of the first image, then warps the second image to fit the first, at least that's what I think is happening
          Imgproc.warpPerspective(secondImage, warpedImage, perspectiveM, new Size(firstImage.width(), firstImage.height()), Imgproc.WARP_INVERSE_MAP, Core.BORDER_CONSTANT);
          // not the nicest solution, but obviously the image's data address changes after but the image it's the same, so to retrieve the same path I had to do this
          this.replaceKey(secondImage.dataAddr(), warpedImage.dataAddr());
          // then to all the things need to create a virtual stack of images
          this.newProcessHandler(warpedImage);
        } else {
          IJ.showMessage("Not enough matches");
        }
      }
    } catch (Exception e) {
      IJ.showMessage("Not all the images will be put in the aligned stack, something went wrong, check your image because it seems that we couldn't find a relation");
    }
  }
  
  @Override
  public void build() {
    try {
      this.setTransformedImagesStack(new ImagePlus("", this.getVirtualStack()));
      String filePath = IJ.getDir(TEMP_PATH) + this.getTransformedImagesStack().hashCode() + TIFF_EXT;
      new ImageConverter(this.getTransformedImagesStack()).convertToRGB();
      new FileSaver(this.getTransformedImagesStack()).saveAsTiff(filePath);
      this.getTempImages().add(filePath);
      this.getLoadingDialog().hideDialog();
      this.setAlignDialog(new AlignDialog(this.getTransformedImagesStack(), this.getListener()));
      this.getAlignDialog().pack();
      this.getAlignDialog().setVisible(true);
    } catch (Exception e) {
      IJ.showMessage(e.getMessage());
    }
    this.getLoadingDialog().hideDialog();
  }
  
  private void newProcessHandler(Mat image) {
    ImageProcessor newProcessor = new ColorProcessor(image.width(), image.height());
    ImagePlus transformedImage = matToImagePlus(image);
    newProcessor.insert(transformedImage.getProcessor(), 0, 0);
    this.addToVirtualStack(new ImagePlus("", newProcessor), this.getVirtualStack());
  }
  
  private Mat getHomography(List<DMatch> goodMatches, List<KeyPoint> firstKeyPoints, List<KeyPoint> secondKeyPoints) {
    final List<Point> obj = new ArrayList<>();
    final List<Point> scene = new ArrayList<>();
    final List<KeyPoint> listOfKeyPointsObject = new ArrayList<>(firstKeyPoints);
    final List<KeyPoint> listOfKeyPointsScene = new ArrayList<>(secondKeyPoints);
    for (int i = 0; i < goodMatches.size(); i++) {
      obj.add(listOfKeyPointsObject.get(goodMatches.get(i).queryIdx).pt);
      scene.add(listOfKeyPointsScene.get(goodMatches.get(i).trainIdx).pt);
    }
    MatOfPoint2f objMat = new MatOfPoint2f();
    MatOfPoint2f sceneMat = new MatOfPoint2f();
    objMat.fromList(obj);
    sceneMat.fromList(scene);
    final double freakThreshold = 0.005;
    return Calib3d.findHomography(objMat, sceneMat, Calib3d.RANSAC, freakThreshold);
  }
  
  private Point[] getPointsArray(Mat firstImage) {
    final Point[] pointsArray = new Point[4];
    final int width = firstImage.width();
    final int height = firstImage.height();
    pointsArray[0] = new Point(0, 0);
    pointsArray[1] = new Point(0, height);
    pointsArray[2] = new Point(width, height);
    pointsArray[3] = new Point(width, 0);
    return pointsArray;
  }
  
  private ImagePlus matToImagePlus(Mat image) {
    final int type = image.type();
    ImageProcessor result = null;
    if (type == CV_8UC1) {
      result = makeByteProcessor(image);
    } else {
      IJ.showMessage("Not supported for now");
    }
    return new ImagePlus("", result);
  }
  
  private ImageProcessor makeByteProcessor(Mat find) {
    final int w = find.cols();
    final int h = find.rows();
    ByteProcessor bp = new ByteProcessor(w, h);
    find.get(0, 0, (byte[]) bp.getPixels());
    return bp;
  }
  
  private void importImages() {
    for (ImageFile imageFile : this.getManager().getImageFiles()) {
      try {
        Mat matImage = imread(imageFile.getPathFile(), IMREAD_GRAYSCALE);
        this.pathMap.put(matImage.dataAddr(), imageFile.getPathFile());
        this.images.add(matImage);
      } catch (Exception e) {
        IJ.showMessage(e.getMessage());
      }
    }
  }
  
  private Mat getDescriptor(Mat image, MatOfKeyPoint keyPoints) {
    final Mat tempDescriptor = new Mat();
    final FREAK extractor = FREAK.create();
    extractor.compute(image, keyPoints, tempDescriptor);
    return tempDescriptor;
  }
  
  private MatOfKeyPoint getKeypoint(Mat image) {
    final MatOfKeyPoint tempKeypoint = new MatOfKeyPoint();
    final StarDetector detector = StarDetector.create();
    detector.detect(image, tempKeypoint);
    return tempKeypoint;
  }
  
  private List<DMatch> getGoodMatches(List<MatOfDMatch> knnMatches) {
    List<DMatch> listOfGoodMatches = new ArrayList<>();
    //-- Filter matches using the Lowe's ratio test
    float ratioThresh = 0.75f;
    for (int i = 0; i < knnMatches.size(); i++) {
      if (knnMatches.get(i).rows() > 1) {
        DMatch[] matches = knnMatches.get(i).toArray();
        if (matches[0].distance < ratioThresh * matches[1].distance) {
          listOfGoodMatches.add(matches[0]);
        }
      }
    }
    return listOfGoodMatches;
  }
  
  private Mat getSourceImage() {
    return this.images.get(0);
  }
  
  private void replaceKey(Long oldAddress, Long newAddress) {
    String value = this.pathMap.get(oldAddress);
    this.pathMap.remove(oldAddress);
    this.pathMap.put(newAddress, value);
  }
}
