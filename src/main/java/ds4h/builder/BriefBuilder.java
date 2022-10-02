/*
 * COPYRIGHT:* Copyright (c) 2021, AUSL Romagna* Azienda USL della Romagna, Italy* All rights reserved.**
 * The informed consent published in integral part on the website of Azienda USL della Romagna
 * (Informed Consent AUSL, prot. N. 1683), must be citied.**
 * This material is free; you can redistribute it and/or modify it under the terms of the CC BY 4.0.*
 * This material is distributed WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package ds4h.builder;

import ds4h.dialog.align.OnAlignDialogEventListener;
import ds4h.dialog.loading.LoadingDialog;
import ds4h.dialog.main.event.AutoAlignEvent;
import ds4h.image.model.manager.ImageFile;
import ds4h.image.model.manager.ImagesEditor;
import ds4h.utils.Pair;
import ij.IJ;
import ij.ImagePlus;
import ij.VirtualStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Point;
import org.opencv.core.*;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.imgproc.Imgproc;
import org.opencv.xfeatures2d.BriefDescriptorExtractor;
import org.opencv.xfeatures2d.StarDetector;

import java.awt.*;
import java.awt.image.ColorModel;
import java.util.List;
import java.util.*;

import static java.lang.Math.abs;
import static org.opencv.core.CvType.CV_8U;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.imgcodecs.Imgcodecs.IMREAD_GRAYSCALE;
import static org.opencv.imgcodecs.Imgcodecs.imreadmulti;
import static org.opencv.imgproc.Imgproc.boundingRect;

/**
 * BriefBuilder is a class implementing the automatic alignment functionality extending abstract builder
 *
 * This plugin is made as a non-profit utility
 * It uses XFeatures2d features like BriefDescriptorExtractor and StartDetector
 * Given that is used in the medical research field and no kind of profit is made from this
 * There isn't any break of the copyright law.
 * For any issues the owners of the algorithms see fit, post an issue here :
 * https://github.com/Edodums/DS4H-Image-Alignment/issues
 */
public class BriefBuilder extends AbstractBuilder<Mat> {
    private final List<Mat> images = new ArrayList<>();
    private final Map<Long, String> pathMap = new HashMap<>();
    private final Map<Pair<Integer, Integer>, Pair<List<Point>, List<Point>>> mapOfPoints = new HashMap<>();
    private final List<Mat> transformedImages = new ArrayList<>();
    private boolean canGo = true;

    // TOCHECK, TOBETESTED
    private double absMaxValue = 0.0;

    public BriefBuilder(LoadingDialog loadingDialog, ImagesEditor editor, AutoAlignEvent event, OnAlignDialogEventListener listener) {
        super(loadingDialog, listener, editor, event);
    }

    @Override
    public void init() {
        this.importImages();
        this.setImagesDimensions(this.getEditor().getImagesDimensions());
        //empty Dimension, this """setMaximumSize""" simply checks if the current maximumSize needs to be updated
        this.setMaximumSize(new Dimension());
        this.setFinalStackDimension(new Dimension(this.getMaximumSize().width, this.getMaximumSize().height));
        this.cacheTransformedImages();
        if (this.getTransformedImages().isEmpty()) {
            canGo = false;
            IJ.showMessage("Not enough matches");
            return;
        }
        this.setOffsets();
        this.findContoursOfTransformedImages();
        this.checkFinalStackDimension();
        this.setFinalStackToVirtualStack();
        this.addFinalStackToVirtualStack();
    }

    private void findContoursOfTransformedImages() {
        System.out.println("CALL findContoursOfTransformedImages");
        System.out.println("Max offsets in findContoursOfTransformedImages are: " + getMaxOffsetX() + " " + getMaxOffsetY());
        this.getFinalStackDimension().height = 0; // not nice, but it works
        this.getTransformedImages().forEach(mat -> {
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            //calculates contours of the images
            Imgproc.findContours(mat, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
            contours.forEach(contour -> {
                Rect rect = boundingRect(contour);
                this.getFinalStackDimension().height = Math.max(this.getFinalStackDimension().height, rect.height);
                this.getFinalStackDimension().width = Math.max(this.getFinalStackDimension().width, rect.width);
            });
            // TOCHECK, TOBETESTED
            // note if you add 450 pixels here, you get twice as big enlargement in the final stack.. why? with '/2' is
            // brutally fixed
            // with more than 2 images to be aligned you get a black band on the left of the stack.
            this.getFinalStackDimension().width = this.getFinalStackDimension().width + (int) this.getAbsMaxValue()/2;
        });
    }

    @Override
    protected ImageProcessor getFinalStackImageProcessor() {
        System.out.println("CALL getFinalStackImageProcessor");
        System.out.println("Max offsets in transformImage are: " + getMaxOffsetX() + " " + getMaxOffsetY());
        final ImageProcessor processor;
        processor = matToImagePlus(this.getSourceImage()).getProcessor().createProcessor(this.getFinalStackDimension().width, this.getFinalStackDimension().height);
        processor.insert(matToImagePlus(this.getSourceImage()).getProcessor(), this.getMaxOffsetX(), this.getMaxOffsetY());
        return processor;
    }

    private void cacheTransformedImages() {
        for (int index = 0; index < this.getImages().size(); index++) {
            Mat transformedImage = this.transformImage(index);
            if (transformedImage == null) {
                continue;
            }
            this.getTransformedImages().add(transformedImage);
        }
    }

    @Override
    public boolean check() {
        return canGo;
    }

    @Override
    public void alignKeepOriginal() {
        System.out.println("CALL alignKeepOriginal");
        // all of the following commented code is a mess. it was supposed to do the keep all pixel data part,
        // by dynamically adapt the size of the imageProcessor, but overcomplicated it with wrongly done offsets.
        // this wrong algorithm created a black band in the left part of the stack in frequent cases,
        // causing my dynamic adaptation to be in vain.
/*        this.setSourceImageIndex(0);
        try {
            for (int index = 0; index < this.getImages().size(); index++) {
                if (index == this.getSourceImageIndex()) continue;
                //take a single transformed image
                Mat image = this.getTransformedImages().get(index);
                if (image == null) continue;
                System.out.println("final stack width in alignKeepOriginal(): " + this.getFinalStackDimension().width);
                System.out.println("final stack height in alignKeepOriginal(): " + this.getFinalStackDimension().height);
                ImageProcessor newProcessor = new ColorProcessor(this.getFinalStackDimension().width, this.getMaximumSize().height);
                ImagePlus transformedImage = this.matToImagePlus(image);
                //transformedImage = transformedImage.
                int offsetXTransformed = 0;
                if (this.getOffsetsX().get(index) > 0 && this.getMaxOffsetXIndex() != index) {
                    offsetXTransformed = abs(this.getOffsetsX().get(index));
                    System.out.println("transformed image offset in alignKeepOriginal(): " + offsetXTransformed);
                }
                offsetXTransformed += this.getMaxOffsetX();
                System.out.println("transformed image offsetXTransformed in alignKeepOriginal(): " + offsetXTransformed);
                //down here images aligned are modified, real deal it's in this two lines
                //newProcessor provides image handling, it gets filled with the information and then passed to the stack
                newProcessor.insert(transformedImage.getProcessor(), offsetXTransformed, this.getMaxOffsetY());
                this.addToVirtualStack(new ImagePlus("", newProcessor));
            }
        } catch (Exception e) {
            IJ.showMessage("Not all the images will be put in the aligned stack, something went wrong, check your image because it seems that we couldn't find a relation: " + e.getMessage());
        }*/
        System.out.println("CALL align");
        System.out.println("Max offsets in transformImage are: " + getMaxOffsetX() + " " + getMaxOffsetY());
        this.setSourceImageIndex(0);
        this.setVirtualStack(new VirtualStack(getSourceImage().width() + (int) getAbsMaxValue(), getSourceImage().height(), ColorModel.getRGBdefault(), IJ.getDir(TEMP_PATH)));
        this.addToVirtualStack(matToImagePlus(getSourceImage()));
        for (int index = 1; index < this.getImages().size(); index++) {
            Mat image = transformImage(index);
            if (image == null) continue;
            ImagePlus transformedImage = this.matToImagePlus(image);
            this.addToVirtualStack(transformedImage);
        }
    }

    @Override
    public void align() {
        System.out.println("CALL align");
        System.out.println("Max offsets in transformImage are: " + getMaxOffsetX() + " " + getMaxOffsetY());
        this.setSourceImageIndex(0);
        this.setVirtualStack(new VirtualStack(getSourceImage().width(), getSourceImage().height(), ColorModel.getRGBdefault(), IJ.getDir(TEMP_PATH)));
        this.addToVirtualStack(matToImagePlus(getSourceImage()));
        for (int index = 1; index < this.getImages().size(); index++) {
            Mat image = transformImage(index);
            if (image == null) continue;
            ImagePlus transformedImage = this.matToImagePlus(image);
            this.addToVirtualStack(transformedImage);
        }
    }

    // here the algorithm is applied to align (transform) the image
    private Mat transformImage(int transformedImageIndex) {
        System.out.println("CALL transformImage");
        // two images
        final Mat firstImage = this.getSourceImage();
        final Mat secondImage = this.getImages().get(transformedImageIndex);
        final MatOfKeyPoint firstKeyPoints = this.getKeypoint(firstImage);
        final MatOfKeyPoint secondKeyPoints = this.getKeypoint(secondImage);
        final Mat firstDescriptor = this.getDescriptor(firstImage, firstKeyPoints);
        final Mat secondDescriptor = this.getDescriptor(secondImage, secondKeyPoints);
        // match
        final DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        // convert to float to use flann ( Brief is a binary descriptor )
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
        List<DMatch> goodMatches = this.getGoodMatches(knnMatches);
        // Below four matches the images couldn't be related
        if (goodMatches.size() > 4) {
            final MatOfPoint2f points = new MatOfPoint2f(this.getPointsArray(firstImage));
            Mat dest = new Mat();
            // Check directly the Javadoc, to learn more
            Core.perspectiveTransform(points, dest, this.getHomography(goodMatches, firstKeyPoints.toList(), secondKeyPoints.toList(), transformedImageIndex));
            final Mat perspectiveM = Imgproc.getPerspectiveTransform(points, dest);
            System.out.println("perspectiveM.width: " + perspectiveM.width());
            System.out.println("perspectiveM.height: " + perspectiveM.height());
            System.out.println("perspectiveM.size: " + perspectiveM.size());
            // TOCHECK, TOBETESTED (BEGINS)
            // with this function you can get min and max value of a Mat object
            Core.MinMaxLocResult minMaxLoc = Core.minMaxLoc(perspectiveM);
            // we get the max absolute value between minVal and maxVal which corresponds to how much larger the
            // transformed (aligned) image should be not to lose information.
            double absMaxVal = Math.max(abs(minMaxLoc.maxVal),abs(minMaxLoc.minVal));
            System.out.println("Max value in the transformation matrix is: " + absMaxVal);
            if(absMaxVal==1){
                absMaxVal = 0.0;
            }
            if(this.getAbsMaxValue() < absMaxVal){
                this.setAbsMaxValue(absMaxVal);
            }
            System.out.println(perspectiveM.dump());
            // TOCHECK, TOBETESTED (ENDS)
            Mat warpedImage = new Mat();
            // Literally takes the secondImage, the perspective transformation matrix, the size of the first image, then warps the second image to fit the first, at least that's what I think is happening
            System.out.println("Max offsets in transformImage are: " + getMaxOffsetX() + " " + getMaxOffsetY());
            // down here the max offset should be added to size (width and height), but it is yet to be calculated:
            // it is to be found a way to calculate it before.
            // TOCHECK, TOBETESTED
            Imgproc.warpPerspective(secondImage, warpedImage, perspectiveM, new Size(this.getFinalStackDimension().width+absMaxVal, this.getMaximumSize().height), Imgproc.WARP_INVERSE_MAP, Core.BORDER_CONSTANT);
            //Imgproc.warpPerspective(secondImage, warpedImage, perspectiveM, new Size(this.getFinalStackDimension().width*2, this.getMaximumSize().height), Imgproc.WARP_INVERSE_MAP, Core.BORDER_CONSTANT);
            // not the nicest solution, but obviously the image's data address changes after but the image it's the same, so to retrieve the same path I had to do this
            this.replaceKey(secondImage.dataAddr(), warpedImage.dataAddr());
            // then to all the things need to create a virtual stack of images
            return warpedImage;
        }
        return null;
    }

    // here you get the homography matrix (matches between images as input, matrix indicating translation as output)
    // which is used in transformImage
    private Mat getHomography(List<DMatch> goodMatches, List<KeyPoint> firstKeyPoints, List<KeyPoint> secondKeyPoints, int indexTransformedImage) {
        final List<Point> obj = new ArrayList<>();
        final List<Point> scene = new ArrayList<>();
        final List<KeyPoint> listOfKeyPointsObject = new ArrayList<>(firstKeyPoints);
        final List<KeyPoint> listOfKeyPointsScene = new ArrayList<>(secondKeyPoints);
        for (DMatch goodMatch : goodMatches) {
            obj.add(listOfKeyPointsObject.get(goodMatch.queryIdx).pt);
            scene.add(listOfKeyPointsScene.get(goodMatch.trainIdx).pt);
        }
        MatOfPoint2f objMat = new MatOfPoint2f();
        MatOfPoint2f sceneMat = new MatOfPoint2f();
        this.getMapOfPoints().put(new Pair<>(this.getSourceImageIndex(), indexTransformedImage), new Pair<>(obj, scene));
        objMat.fromList(obj);
        sceneMat.fromList(scene);
        final double briefThreshold = 0.005;
        return Calib3d.findHomography(objMat, sceneMat, Calib3d.RANSAC, briefThreshold);
    }

    /**
     *
     * @param firstImage Input image
     * @return array of Point containing the coordinates of the 4 corners of the image
     */
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

    /**
     *
     * imports the images from editor into the builder
     */
    private void importImages() {
        for (ImageFile imageFile : this.getEditor().getImageFiles()) {
            try {
                List<Mat> images = new ArrayList<>();
                imreadmulti(imageFile.getPathFile(), images, IMREAD_GRAYSCALE);
                images.forEach(image -> this.pathMap.put(image.dataAddr(), imageFile.getPathFile()));
                this.getImages().addAll(images);
            } catch (Exception e) {
                IJ.showMessage(e.getMessage());
            }
        }
    }

    private Mat getDescriptor(Mat image, MatOfKeyPoint keyPoints) {
        final Mat tempDescriptor = new Mat();
        final BriefDescriptorExtractor extractor = BriefDescriptorExtractor.create(32, true);
        extractor.compute(image, keyPoints, tempDescriptor);
        return tempDescriptor;
    }

    /**
     *
     * @param image the image of which keypoints are needed
     * @return a MatOfKeyPoint variable containing keypoints of the image
     */
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
        for (MatOfDMatch knnMatch : knnMatches) {
            if (knnMatch.rows() > 1) {
                DMatch[] matches = knnMatch.toArray();
                if (matches[0].distance < ratioThresh * matches[1].distance) {
                    listOfGoodMatches.add(matches[0]);
                }
            }
        }
        return listOfGoodMatches;
    }

    @Override
    protected Mat getSourceImage() {
        return this.getImages().get(this.getSourceImageIndex());
    }

    private void replaceKey(Long oldAddress, Long newAddress) {
        String value = this.pathMap.get(oldAddress);
        this.pathMap.remove(oldAddress);
        this.pathMap.put(newAddress, value);
    }

    private void setOffsets() {
        System.out.println("CALL setOffsets");
        System.out.println("Max offsets in transformImage are: " + getMaxOffsetX() + " " + getMaxOffsetY());
        this.setOffsetsX(new ArrayList<>());
        this.setOffsetsY(new ArrayList<>());
        for (int index = 0; index < this.getMapOfPoints().size(); index++) {
            if (index == this.getSourceImageIndex()) {
                this.getOffsetsX().add(0);
                this.getOffsetsY().add(0);
                continue;
            }
            Pair<List<Point>, List<Point>> pointsPair = this.getMapOfPoints().get(new Pair<>(this.getSourceImageIndex(), index));
            Point point = pointsPair.getFirst().get(0);
            this.getOffsetsX().add((int) (point.x - pointsPair.getSecond().get(0).x));
            this.getOffsetsY().add((int) (point.y - pointsPair.getSecond().get(0).y));
        }
        Optional<Integer> optMaxX = this.getOffsetsX().stream().max(Comparator.naturalOrder());
        Optional<Integer> optMaxY = this.getOffsetsY().stream().max(Comparator.naturalOrder());
        optMaxX.ifPresent(this::setMaxOffsetX);
        optMaxY.ifPresent(this::setMaxOffsetY);
        this.setMaxOffsetXIndex(this.getOffsetsX().indexOf(this.getMaxOffsetX()));
        if (this.getMaxOffsetX() <= 0) {
            this.setMaxOffsetX(0);
            this.setMaxOffsetXIndex(-1);
        }
        this.setMaxOffsetYIndex(this.getOffsetsY().indexOf(this.getMaxOffsetY()));
        if (this.getMaxOffsetY() <= 0) {
            this.setMaxOffsetY(0);
            // the following line was absent in the version I received,
            // it was probably a mistake but could've also been for some reason
            this.setMaxOffsetYIndex(-1);
        }
    }

    public Map<Pair<Integer, Integer>, Pair<List<Point>, List<Point>>> getMapOfPoints() {
        return mapOfPoints;
    }

    public List<Mat> getTransformedImages() {
        return transformedImages;
    }

    public List<Mat> getImages() {
        return images;
    }
    // TOCHECK, TOBETESTED
    private void setAbsMaxValue(Double newValue){
        this.absMaxValue = newValue;
    }
    // TOCHECK, TOBETESTED
    private double getAbsMaxValue(){
        return this.absMaxValue;
    }
    /**
     *
     * @return size the final size of the stack, with aligned images and all data preserved
     */
    private Size getStackFinalSize(){
        Size size = new Size(0,0);
        for(Mat img: getTransformedImages()){
            //method to find the max extension of aligned images while keeping all pixels
            //this will be used to adapt the output of automatic alignment, when keep all pixel data is activated
            // in order to not cut the images.
        }
        return size;
    }
}
