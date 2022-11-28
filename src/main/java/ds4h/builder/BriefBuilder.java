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
import ij.plugin.RGBStackMerge;
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
import static org.opencv.imgcodecs.Imgcodecs.*;

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
    private List<List<Mat>> imagesSplit = new ArrayList<List<Mat>>();
    private final Map<Long, String> pathMap = new HashMap<>();
    private final Map<Pair<Integer, Integer>, Pair<List<Point>, List<Point>>> mapOfPoints = new HashMap<>();
    private final List<Mat> transformedImages = new ArrayList<>();
    private boolean canGo = true;

    // TOCHECK, TOBETESTED
    private List<Integer> horizShifts = new ArrayList<>();
    private List<Integer> verticShifts = new ArrayList<>();
    private boolean alignCalled = false;
    private int maxXshift = 0;
    private int maxYshift = 0;
    private List<Integer> imagesShiftList = new ArrayList<>();

    public BriefBuilder(LoadingDialog loadingDialog, ImagesEditor editor, AutoAlignEvent event, OnAlignDialogEventListener listener) {
        super(loadingDialog, listener, editor, event);
    }

    /**
     * Calls methods and function to compute and provide the aligned stack.
     */
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
        //this.findContoursOfTransformedImages();
        this.checkFinalStackDimension();
        this.setFinalStackToVirtualStack();
        this.addFinalStackToVirtualStack();
        alignCalled = false;
    }

/*    // this function is probably useless now, the virtualstack dimension are set in alignKeepOriginal
    private void findContoursOfTransformedImages() {
        System.out.println("CALL findContoursOfTransformedImages");
        System.out.println("Max offsets in findContoursOfTransformedImages are: " + getMaxXshift() + " " + getMaxYshift());
        this.getFinalStackDimension().width = this.getFinalStackDimension().width + (int) this.getMaxXshift();
        this.getFinalStackDimension().height = this.getFinalStackDimension().height + (int) this.getMaxYshift();
    }*/

    @Override
    protected ImageProcessor getFinalStackImageProcessor() {
        final ImageProcessor processor;
        processor = matToImagePlus(this.getSourceImage()).getProcessor().createProcessor(this.getFinalStackDimension().width, this.getFinalStackDimension().height);
        processor.insert(matToImagePlus(this.getSourceImage()).getProcessor(), this.getMaxOffsetX(), this.getMaxOffsetY());
        return processor;
    }

    /**
     * for each image calls beforehand transformImage(), to achieve information about the images position and shifts
     */
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

    /**
     * Aligns the uploaded images, dynamically adapting the output stack dimensions,
     * therefore keeping all information.
     */
    @Override
    public void alignKeepOriginal() {
        // alignCalled is needed because transformImage() is also used as a caching function
        // to get information about the shifts, so some operations must not be called when caching.
        alignCalled = true;
        List<ImagePlus> transformedImagesList = new ArrayList<>();
        // we want the source image to be the most to the left of the aligned stack otherwise
        // the algorithm cuts the images out of the resulting stack.
        this.setSourceImageIndex(getLeftMostImageIndex());
        // use transformImage() for each image, then convert it to ImagePlus
        for (int index = 0; index < this.getImages().size(); index++) {
            Mat image = transformImage(index);
            if (image == null) continue;
            ImagePlus transformedImage = this.matToImagePlus(image);
            transformedImagesList.add(transformedImage);
        }
        // VirtualStack dimension will be adapted from source image, adding the maximum horizontal and vertical shifts.
        this.setVirtualStack(new VirtualStack(getSourceImage().width() + getMaxXshift(), this.getSourceImage().height() + getMaxYshift(), ColorModel.getRGBdefault(), IJ.getDir(TEMP_PATH)));
        for(int i = 0; i < transformedImagesList.size(); i++){
            //if it is empty, it means the image was grayscale
            if(imagesSplit.get(i).isEmpty()){
                this.addToVirtualStack(new ImagePlus("", transformedImagesList.get(i).getProcessor()));
            } else {
                this.addToVirtualStack(new ImagePlus("", matToRGBImagePlus(imagesSplit.get(i)).getProcessor()));
            }
        }
    }

    /**
     *
     * @return the index of the image which is most to the left in the final stack, which will be source image
     */
    private int getLeftMostImageIndex() {
        int max = Collections.max(horizShifts);
        return horizShifts.indexOf(max);
    }

    /**
     * aligns provided images but cutting them to the size of the source image (no stack adaptation)
     */
    @Override
    public void align() {
        alignCalled = true;
        this.setSourceImageIndex(0);
        // virtual stack size fixed to source image width and height
        this.setVirtualStack(new VirtualStack(getSourceImage().width(), getSourceImage().height(), ColorModel.getRGBdefault(), IJ.getDir(TEMP_PATH)));
        ArrayList<Mat> sourceSplitRGB = new ArrayList<>();
        Core.split(getSourceImage(), sourceSplitRGB);
        // source image will be the first image of the output stack, the others will be aligned subsequently
        if(!imagesSplit.get(getSourceImageIndex()).isEmpty()){
            this.addToVirtualStack(matToRGBImagePlus(imagesSplit.get(getSourceImageIndex())));
        } else if(getSourceImage().channels() == 1){
            this.addToVirtualStack(matToImagePlus(getSourceImage()));
        } else if(getSourceImage().channels() != 1){
            IJ.showMessage("Only grayscale and RGB formats are currently supported");
        }
        // call transformImage to get the aligned stack
        List<ImagePlus> transformedImagesList = new ArrayList<>();
        for (int index = 0; index < this.getImages().size(); index++) {
            Mat image = transformImage(index);
            if (image == null) continue;
            ImagePlus transformedImage = this.matToImagePlus(image);
            transformedImagesList.add(transformedImage);
        }
        // starting from one because earlier we already inserted the first image.
        // in case you want to let the user choose the source image, do a for cycle excluding sourceImageIndex()
        for(int i = 0; i < transformedImagesList.size(); i++){
            // first if to exclude the source image, since it had already been added some lines earlier.
            if(i != getSourceImageIndex()){
                //if imageSplit array is empty, it means the image was grayscale and its channels weren't added
                if(imagesSplit.get(i).isEmpty()){
                    this.addToVirtualStack(new ImagePlus("", transformedImagesList.get(i).getProcessor()));
                } else {
                    this.addToVirtualStack(new ImagePlus("", matToRGBImagePlus(imagesSplit.get(i)).getProcessor()));
                }
            }
        }
    }

    /**
     * transform an image as Mat, aligning it with the one taken as source using a keypoint algorithm
     * @param transformedImageIndex the index of the image to be aligned
     * @return the image, as Mat, now aligned
     */
    private Mat transformImage(int transformedImageIndex) {
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
            IJ.showMessage("Check all your images, one of them seems to have no valuable matches for our algorithm");
        }
        List<DMatch> goodMatches = this.getGoodMatches(knnMatches);
        // Below four matches the images couldn't be related
        if (goodMatches.size() > 4) {
            final MatOfPoint2f points = new MatOfPoint2f(this.getPointsArray(firstImage));
            Mat dest = new Mat();
            // Check directly the Javadoc, to learn more
            Core.perspectiveTransform(points, dest, this.getHomography(goodMatches, firstKeyPoints.toList(), secondKeyPoints.toList(), transformedImageIndex));
            Mat perspectiveM = Imgproc.getPerspectiveTransform(points, dest);
            Mat warpedImage = applyWarping(secondImage, transformedImageIndex, perspectiveM);
            // then to all the things need to create a virtual stack of images
            return warpedImage;
        }
        return null;
    }

    /**
     *
     * @param image the Mat (image) to be warped
     * @param imageIndex the image index
     * @param perspectiveM the perspective transformation matrix necessary for the warping
     * @return the warped Mat
     */
    private Mat applyWarping(Mat image, int imageIndex, Mat perspectiveM){
        // images higher than sourceImage get partially cut, so this is to shift
        // the whole stack vertically of the necessary amount
        perspectiveM.put(1,2, perspectiveM.get(1,2)[0] - getSourceVerticalShift());
        // Get horizontal and vertical shift of the image from the perspectiveMatrix (do a matrix dump to check it out)
        // if it's < 1 then it's either zero or the matrix calculation error equivalent to 0
        int imgHorizontalShift = (abs((int) perspectiveM.get(0,2)[0])) < 1 ? 0 : ((int) perspectiveM.get(0,2)[0]);
        int imgVerticalShift = (abs((int) perspectiveM.get(1,2)[0])) < 1 ? 0 : ((int) perspectiveM.get(1,2)[0]);
        if(!alignCalled){
            horizShifts.add(imageIndex, imgHorizontalShift);
            verticShifts.add(imageIndex, imgVerticalShift);
        }
        // check and update the max shifts, if needed
        if(this.getMaxXshift() < abs(imgHorizontalShift)){
            this.setMaxXshift(abs(imgHorizontalShift));
        }
        if(this.getMaxYshift() < abs(imgVerticalShift)){
            this.setMaxYshift(abs(imgVerticalShift));
        }
        Mat warpedImage = new Mat();
        //first check if align function was called, not to shift twice
        // (it gets called by cachetransformedImage(), by alignKeepOriginal() or by align()).
        if (alignCalled) {
            // if the array is not empty, it means it got filled because the image was rgb.
            if(!imagesSplit.get(imageIndex).isEmpty()){
                alignImageChannels(imageIndex, perspectiveM);
            }
        }
        // Takes image to which apply transformation, output image, the perspective transformation matrix,
        // the size of the output image, then warps the image using the matrix
        Imgproc.warpPerspective(image, warpedImage, perspectiveM, new Size(this.getFinalStackDimension().width + this.getMaxXshift(), this.getMaximumSize().height + this.getMaxYshift()), Imgproc.WARP_INVERSE_MAP, Core.BORDER_CONSTANT);
        // not the nicest solution, but obviously the image's data address changes after but the image it's the same, so to retrieve the same path I had to do this
        this.replaceKey(image.dataAddr(), warpedImage.dataAddr());
        return warpedImage;
    }

    /**
     * Aligns rgb image channels, based on a perspective transform matrix
     * @param imageIndex the index of the rgb image
     * @param perspectiveTransform the perspective transform matrix used to align the image
     */
    private void alignImageChannels(int imageIndex, Mat perspectiveTransform){
        List<Mat> warpedChannelList = new ArrayList<>();
        // shift (align) all the rgb channels one by one
        for(int j = 0; j < imagesSplit.get(imageIndex).size(); j++){
            Mat warpedChannel = new Mat();
            // down here the max offset is added to size (width and height), this is possible because transformImage
            // gets called already nImages times, and the max shifts are already calculated as well.
            Imgproc.warpPerspective(imagesSplit.get(imageIndex).get(j), warpedChannel, perspectiveTransform, new Size(this.getFinalStackDimension().width + this.getMaxXshift(), this.getMaximumSize().height + this.getMaxYshift()), Imgproc.WARP_INVERSE_MAP, Core.BORDER_CONSTANT);
            warpedChannelList.add(warpedChannel);
        }
        // replace the old rgb channels with the aligned ones.
        imagesSplit.get(imageIndex).clear();
        imagesSplit.get(imageIndex).addAll(warpedChannelList);
    }

    /**
     *
     * @return the vertical shift between the source image and the higher image in the aligned stack
     */
    private double getSourceVerticalShift() {
        return verticShifts.size() == 0? 0 : (double) Collections.max(verticShifts) - verticShifts.get(getSourceImageIndex());
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

    /**
     *
     * @param image in Mat format to be converted to ImagePlus (only works with grayscale CV_8UC1 Mats)
     * @return image in ImagePlus format
     */
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
                // we add an empty array to imagesSplit, in case the images are RGB,
                // An empty array is added, in case the image is RGB it gets filled with the channels as Mat,
                // otherwise it stays empty. The empty check is done in transform image.
                this.imagesSplit.add(new ArrayList<Mat>());
                Mat singleImage = imread(imageFile.getPathFile(), IMREAD_ANYCOLOR);
                // in case image is rgb, split the image in its different Mat channels,
                // to be then aligned and merged and fill the splitImages array
                if(singleImage.channels() == 3){
                    Core.split(singleImage, imagesSplit.get(this.getEditor().getImageFiles().indexOf(imageFile)));
                } else if(singleImage.channels() != 1){
                    IJ.showMessage("Only grayscale and RGB formats are currently supported");
                }
                // the alignment is handled in grayscale, provides the aligned stack information that we will eventually use
                // to align the rgb images, which have been separated into mat channels.
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

    /**
     *
     * @param rgbChannels List of 3 Mat objects, channels of a RGB image to be converted in ImagePlus and merged
     * @return the resulting ImagePlus
     */
    private ImagePlus matToRGBImagePlus(List<Mat> rgbChannels){
        if(rgbChannels.size() != 3){
            IJ.showMessage("Only RGB images with 3 channels accepted as input");
            return null;
        }
        List<ImagePlus> imagePlusChannels = new ArrayList<>();
        // I convert the rgb mat channels into imagePlus
        ImagePlus channel;
        for (Mat el: rgbChannels) {
            channel = matToImagePlus(el);
            imagePlusChannels.add(channel);
        }
        ImagePlus mergedColoredImage = new ImagePlus();
        // I merge the 3 rgb single channels into one, now consisting in the rgb image
        mergedColoredImage.setStack(RGBStackMerge.mergeStacks(imagePlusChannels.get(2).getImageStack(), imagePlusChannels.get(1).getImageStack(), imagePlusChannels.get(0).getImageStack(), false));
        return  mergedColoredImage;
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

    /**
     *
     * @param newValue sets the max shift in the stack in order to adapt its width
     */
    private void setMaxXshift(int newValue){
        this.maxXshift = newValue;
    }

    // TOCHECK, TOBETESTED
    /**
     *
     * @return the max shift in the final stack, used to adapt its width
     */
    private int getMaxXshift(){
        return this.maxXshift;
    }

    /**
     *
     * @param newValue sets the max shift in the stack in order to adapt its width
     */
    private void setMaxYshift(int newValue){
        this.maxYshift = newValue;
    }

    // TOCHECK, TOBETESTED
    /**
     *
     * @return the max shift in the final stack, used to adapt its width
     */
    private int getMaxYshift(){
        return this.maxYshift;
    }
}
