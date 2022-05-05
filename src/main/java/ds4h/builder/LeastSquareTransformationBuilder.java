package ds4h.builder;

import ds4h.dialog.align.AlignDialog;
import ds4h.dialog.align.OnAlignDialogEventListener;
import ds4h.dialog.align.setting.SettingDialog;
import ds4h.dialog.loading.LoadingDialog;
import ds4h.dialog.main.event.AlignEvent;
import ds4h.dialog.main.event.IMainDialogEvent;
import ds4h.image.buffered.BufferedImage;
import ds4h.image.manager.ImagesManager;
import ds4h.image.registration.LeastSquareImageTransformation;
import ij.IJ;
import ij.ImagePlus;
import ij.VirtualStack;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.plugin.frame.RoiManager;
import ij.process.ColorProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.image.ColorModel;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class LeastSquareTransformationBuilder extends AbstractBuilder {
  private int sourceImageIndex = -1;
  private List<Dimension> imagesDimensions;
  private List<RoiManager> managers;
  private List<Integer> offsetsX;
  private List<Integer> offsetsY;
  private SettingDialog settingDialog;
  private BufferedImage sourceImage;
  private Integer maxOffsetX;
  private Integer maxOffsetY;
  private int maxOffsetXIndex;
  private int maxOffsetYIndex;
  private int edgeX;
  private int edgeY;
  private int edgeX2;
  private int edgeY2;
  
  
  public LeastSquareTransformationBuilder(LoadingDialog loadingDialog, ImagesManager manager, IMainDialogEvent event, OnAlignDialogEventListener listener) {
    super(loadingDialog, listener, manager, event);
  }
  
  @Override
  public void init() {
    this.setImagesDimensions(this.getManager().getImagesDimensions());
    this.setMaximumSize(new Dimension());
    this.setSourceImage(true);
    this.setOffsets();
    this.initFinalStack();
    this.showSettingDialog();
  }
  
  private void showSettingDialog() {
    try {
      SwingUtilities.invokeAndWait(() -> {
        settingDialog = new SettingDialog(new JFrame(), "Align Settings", true);
        settingDialog.getOkButton().addActionListener(e -> settingDialog.dispose());
        settingDialog.init();
      });
    } catch (InterruptedException | InvocationTargetException e) {
      IJ.showMessage("Something is not right, sorry, contact the Developer");
    }
  }
  
  @Override
  public void align() {
    for (int index = 0; index < this.getManager().getNImages(); index++) {
      if (index == this.getSourceImageIndex()) continue;
      ImageProcessor newProcessor = new ColorProcessor(this.getMaximumSize().width, this.getMaximumSize().height);
      ImagePlus transformedImage = LeastSquareImageTransformation.transform(this.getManager().getOriginal(index, true), this.getSourceImage(), this.getSettingDialog().getEvent());
      BufferedImage transformedOriginalImage = this.getManager().getOriginal(index, true);
      this.setEdges(transformedOriginalImage);
      int offsetXOriginal = 0;
      if (this.offsetsX.get(index) < 0) {
        offsetXOriginal = Math.abs(offsetsX.get(index));
      }
      offsetXOriginal += maxOffsetXIndex != index ? maxOffsetX : 0;
      int offsetXTransformed = 0;
      if (offsetsX.get(index) > 0 && maxOffsetXIndex != index) {
        offsetXTransformed = Math.abs(offsetsX.get(index));
      }
      offsetXTransformed += maxOffsetX;
      int difference = (int) (this.getManagers().get(maxOffsetYIndex).getRoisAsArray()[0].getYBase() - this.getManagers().get(index).getRoisAsArray()[0].getYBase());
      newProcessor.insert(transformedOriginalImage.getProcessor(), offsetXOriginal, difference);
      if (transformedImage != null) {
        newProcessor.insert(transformedImage.getProcessor(), offsetXTransformed, (maxOffsetY));
      }
      this.addToVirtualStack(new ImagePlus("", newProcessor), this.getVirtualStack());
    }
  }
  
  @Override
  public void build() {
    AlignEvent event = (AlignEvent) this.getEvent();
    if (event.isKeepOriginal()) {
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
    } else {
      BufferedImage sourceImg = this.getManager().getOriginal(0, true);
      VirtualStack virtualStack = new VirtualStack(sourceImg.getWidth(), sourceImg.getHeight(), ColorModel.getRGBdefault(), IJ.getDir(TEMP_PATH));
      this.addToVirtualStack(sourceImg, virtualStack);
      for (int index = 1; index < this.getManager().getNImages(); index++) {
        ImagePlus transformedImage = LeastSquareImageTransformation.transform(this.getManager().getOriginal(index, true), sourceImg, this.getSettingDialog().getEvent());
        if (transformedImage != null) {
          this.addToVirtualStack(transformedImage, virtualStack);
        }
      }
    }
  }
  
  @Override
  public void setMaximumSize(Dimension maximumSize) {
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
    super.setMaximumSize(maximumSize);
  }
  
  private Dimension getFinalStack() {
    Dimension finalStack = new Dimension(this.getMaximumSize().width, this.getMaximumSize().height);
    finalStack.width = finalStack.width + this.getMaxOffsetX();
    finalStack.height += this.getSourceImage().getHeight() == this.getMaximumSize().height ? this.getMaxOffsetY() : 0;
    // The final stack of the image is exceeding the maximum size of the images for imagej (see http://imagej.1557.x6.nabble.com/Large-image-td5015380.html)
    if (((double) finalStack.width * finalStack.height) > Integer.MAX_VALUE) {
      JOptionPane.showMessageDialog(null, IMAGE_SIZE_TOO_BIG, IMAGE_SIZE_TOO_BIG_TITLE, JOptionPane.ERROR_MESSAGE);
      this.getLoadingDialog().hideDialog(); // take care of this
      return null;
    }
    return finalStack;
  }
  
  private ImageProcessor getFinalStackImageProcessor(Dimension finalStack) {
    final ImageProcessor processor;
    processor = this.getSourceImage().getProcessor().createProcessor(finalStack.width, finalStack.height);
    processor.insert(this.getSourceImage().getProcessor(), this.getMaxOffsetX(), this.getMaxOffsetY());
    return processor;
  }
  
  private void setOffsets() {
    this.offsetsX = new ArrayList<>();
    this.offsetsY = new ArrayList<>();
    this.managers = this.getManager().getRoiManagers();
    for (int i = 0; i < this.getManagers().size(); i++) {
      if (i == this.getSourceImageIndex()) {
        this.getOffsetsX().add(0);
        this.getOffsetsY().add(0);
        continue;
      }
      Roi roi = this.getManagers().get(i).getRoisAsArray()[0];
      this.getOffsetsX().add((int) (roi.getXBase() - this.getSourceImage().getManager().getRoisAsArray()[0].getXBase()));
      this.getOffsetsY().add((int) (roi.getYBase() - this.getSourceImage().getManager().getRoisAsArray()[0].getYBase()));
    }
    Optional<Integer> optMaxX = this.getOffsetsX().stream().max(Comparator.naturalOrder());
    Optional<Integer> optMaxY = this.getOffsetsY().stream().max(Comparator.naturalOrder());
    optMaxX.ifPresent(this::setMaxOffsetX);
    optMaxY.ifPresent(this::setMaxOffsetY);
    this.maxOffsetXIndex = this.getOffsetsX().indexOf(this.getMaxOffsetX());
    if (maxOffsetX <= 0) {
      this.maxOffsetX = 0;
      this.maxOffsetXIndex = -1;
    }
    this.maxOffsetYIndex = this.getOffsetsY().indexOf(this.getMaxOffsetY());
    if (maxOffsetY <= 0) {
      this.maxOffsetY = 0;
    }
  }
  
  private List<Dimension> getImagesDimensions() {
    return this.imagesDimensions;
  }
  
  private void setImagesDimensions(List<Dimension> imagesDimensions) {
    this.imagesDimensions = new ArrayList<>(imagesDimensions);
  }
  
  private int getSourceImageIndex() {
    return this.sourceImageIndex;
  }
  
  private BufferedImage getSourceImage() {
    return this.sourceImage;
  }
  
  @SuppressWarnings("SameParameterValue")
  private void setSourceImage(boolean isWholeSlide) {
    this.sourceImage = this.getManager().getOriginal(this.getSourceImageIndex(), isWholeSlide);
  }
  
  private Integer getMaxOffsetX() {
    return this.maxOffsetX;
  }
  
  private void setMaxOffsetX(Integer maxOffsetX) {
    this.maxOffsetX = maxOffsetX;
  }
  
  private Integer getMaxOffsetY() {
    return this.maxOffsetY;
  }
  
  private void setMaxOffsetY(Integer maxOffsetY) {
    this.maxOffsetY = maxOffsetY;
  }
  
  private int getEdgeX() {
    return this.edgeX;
  }
  
  private int getEdgeY() {
    return this.edgeY;
  }
  
  private int getEdgeX2() {
    return this.edgeX2;
  }
  
  private int getEdgeY2() {
    return this.edgeY2;
  }
  
  private List<Integer> getOffsetsX() {
    return this.offsetsX;
  }
  
  private List<Integer> getOffsetsY() {
    return this.offsetsY;
  }
  
  private List<RoiManager> getManagers() {
    return this.managers;
  }
  
  private SettingDialog getSettingDialog() {
    return this.settingDialog;
  }
  
  private void initFinalStack() {
    Dimension finalStack = this.getFinalStack();
    if (finalStack != null) {
      final ImageProcessor processor = this.getFinalStackImageProcessor(finalStack);
      this.setVirtualStack();
      this.addToVirtualStack(new ImagePlus("", processor), this.getVirtualStack());
    }
  }
  
  private void setEdges(BufferedImage transformedOriginalImage) {
    this.edgeX = -1;
    this.edgeY = -1;
    for (Roi points : transformedOriginalImage.getManager().getRoisAsArray()) {
      if (this.getEdgeX() == -1 || this.getEdgeX() > points.getXBase()) {
        this.edgeX = (int) points.getXBase();
      }
      if (this.getEdgeY() == -1 || this.getEdgeY() > points.getYBase()) {
        this.edgeY = (int) points.getYBase();
      }
    }
    this.edgeX2 = -1;
    this.edgeY2 = -1;
    for (Roi roi : getSourceImage().getManager().getRoisAsArray()) {
      if (this.getEdgeX2() == -1 || this.getEdgeX2() > roi.getXBase()) {
        this.edgeX2 = (int) roi.getXBase();
      }
      if (this.getEdgeY2() == -1 || this.getEdgeY2() > roi.getYBase()) {
        this.edgeY2 = (int) roi.getYBase();
      }
    }
  }
}
