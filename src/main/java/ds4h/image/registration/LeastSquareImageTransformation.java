package ds4h.image.registration;

import ds4h.dialog.align.setting.SettingEvent;
import ds4h.image.buffered.BufferedImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import mpicbg.ij.Mapping;
import mpicbg.ij.TransformMeshMapping;
import mpicbg.models.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LeastSquareImageTransformation {
  public static final int MINIMUM_ROI_NUMBER = 3;
  
  private LeastSquareImageTransformation() {}
  
  /**
   * Performs the least square transformation between two BufferedImages with a series of fixed parameters.
   */
  public static ImagePlus transform(BufferedImage source, BufferedImage template, SettingEvent event) {
    Mapping<?> mapping;
    final MovingLeastSquaresTransform t = new MovingLeastSquaresTransform();
    try {
      Class<? extends Model<?>> modelClass = LeastSquareImageTransformation.getTransformationModel(event);
      t.setModel(modelClass);
    } catch (Exception e) {
      IJ.showMessage(e.getMessage());
    }
    t.setAlpha(1.0f);
    int meshResolution = 32;
    final ImagePlus target = template.createImagePlus();
    final ImageProcessor ipSource = source.getProcessor();
    final ImageProcessor ipTarget = source.getProcessor().createProcessor(template.getWidth(), template.getHeight());
    final List<Point> sourcePoints = Arrays.stream(source.getManager().getRoisAsArray()).map(roi -> {
      double oldX = roi.getXBase();
      double oldY = roi.getYBase();
      double newX = oldX * ((double) source.getWidth() / source.getEditorImageDimension().width);
      double newY = oldY * ((double) source.getHeight() / source.getEditorImageDimension().height);
      return new Point(new double[]{newX, newY});
    }).collect(Collectors.toList());
    final List<Point> templatePoints = Arrays.stream(template.getManager().getRoisAsArray()).map(roi -> {
      double oldX = roi.getXBase();
      double oldY = roi.getYBase();
      double newX = oldX * ((double) template.getWidth() / template.getEditorImageDimension().width);
      double newY = oldY * ((double) template.getHeight() / template.getEditorImageDimension().height);
      return new Point(new double[]{newX, newY});
    }).collect(Collectors.toList());
    final int numMatches = Math.min(sourcePoints.size(), templatePoints.size());
    final ArrayList<PointMatch> matches = new ArrayList<>();
    for (int i = 0; i < numMatches; ++i)
      matches.add(new PointMatch(sourcePoints.get(i), templatePoints.get(i)));
    try {
      t.setMatches(matches);
      mapping = new TransformMeshMapping<>(new CoordinateTransformMesh(t, meshResolution, source.getWidth(), source.getHeight()));
    } catch (final Exception e) {
      IJ.showMessage(e.getMessage());
      IJ.showMessage("Not enough landmarks selected to find a transformation model.");
      return null;
    }
    ipSource.setInterpolationMethod(ImageProcessor.BICUBIC);
    mapping.mapInterpolated(ipSource, ipTarget);
    target.setProcessor("Transformed" + source.getTitle(), ipTarget);
    return target;
  }
  
  private static Class<? extends Model<?>> getTransformationModel(SettingEvent event) {
    Class<? extends Model<?>> model = AffineModel2D.class;
    if (event.isRigid()) {
      model = TranslationModel2D.class;
    }
    if (event.isProjective()) {
      // THIS ONE NEEDS FOUR ROIS
      model = HomographyModel2D.class;
    }
    return model;
  }
  
  /**
   * UNUSED, TO DELETE IF YOU DON'T FIND ANY PURPOSE
   */
  public static ImagePlus convertToStack(ImagePlus[] images, int count, int width, int height) {
    double min = Double.MAX_VALUE;
    double max = -Double.MAX_VALUE;
    ImageStack stack = new ImageStack(width, height);
    for (int i = 0; i < count; i++) {
      ImageProcessor ip = images[i].getProcessor();
      if (ip.getMin() < min) min = ip.getMin();
      if (ip.getMax() > max) max = ip.getMax();
      if (ip.getWidth() != width || ip.getHeight() != height) {
        ImageProcessor ip2 = new ColorProcessor(width, height);
        int xOff = 0;
        int yOff = 0;
        ip2.insert(ip, xOff, yOff);
        ip = ip2;
      }
      stack.addSlice("", ip);
    }
    ImagePlus imp = new ImagePlus("", stack);
    imp.getProcessor().setMinAndMax(min, max);
    return imp;
  }
}
