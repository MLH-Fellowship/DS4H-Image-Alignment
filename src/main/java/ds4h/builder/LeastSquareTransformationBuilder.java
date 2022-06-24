package ds4h.builder;

import ds4h.dialog.align.AlignDialog;
import ds4h.dialog.align.OnAlignDialogEventListener;
import ds4h.dialog.align.setting.SettingDialog;
import ds4h.dialog.loading.LoadingDialog;
import ds4h.dialog.main.event.MainDialogEvent;
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
    private List<RoiManager> managers;
    private SettingDialog settingDialog;
    private BufferedImage sourceImage;


    public LeastSquareTransformationBuilder(LoadingDialog loadingDialog, ImagesManager manager, MainDialogEvent event, OnAlignDialogEventListener listener) {
        super(loadingDialog, listener, manager, event);
    }

    @Override
    public void init() {
        this.setMaximumSize(new Dimension());
        this.setSourceImage(true);
        this.setOffsets();
        this.initFinalStack();
    }

    @Override
    public boolean check() {
        return isShowSettingDialogSuccessful();
    }

    /**
     * @return if it's "successful" if it wasn't closed via X button ( yeah, I know, it's not elegant )
     */
    private boolean isShowSettingDialogSuccessful() {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        final boolean[] isSuccessful = {true};
        try {
            SwingUtilities.invokeAndWait(() -> {
                settingDialog = new SettingDialog(frame, "Align Settings", true);
                settingDialog.getOkButton().addActionListener(e -> settingDialog.dispose());
                if (!settingDialog.initIsSuccessFul()) {
                    isSuccessful[0] = false;
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            IJ.showMessage("Something is not right, sorry, contact the Developer");
        }

        return isSuccessful[0];
    }

    @Override
    public void align() {
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

    @Override
    public void alignKeepOriginal() {
        for (int index = 0; index < this.getManager().getNImages(); index++) {
            if (index == this.getSourceImageIndex()) continue;
            ImageProcessor newProcessor = new ColorProcessor(this.getMaximumSize().width, this.getMaximumSize().height);
            ImagePlus transformedImage = LeastSquareImageTransformation.transform(this.getManager().getOriginal(index, true), this.getSourceImage(), this.getSettingDialog().getEvent());
            BufferedImage transformedOriginalImage = this.getManager().getOriginal(index, true);
            this.setEdges(transformedOriginalImage);
            int offsetXOriginal = 0;
            if (this.getOffsetsX().get(index) < 0) {
                offsetXOriginal = Math.abs(this.getOffsetsX().get(index));
            }
            offsetXOriginal += this.getMaxOffsetXIndex() != index ? this.getMaxOffsetX() : 0;
            int offsetXTransformed = 0;
            if (this.getOffsetsX().get(index) > 0 && this.getMaxOffsetXIndex() != index) {
                offsetXTransformed = Math.abs(this.getOffsetsX().get(index));
            }
            offsetXTransformed += this.getMaxOffsetX();
            int difference = (int) (this.getManagers().get(this.getMaxOffsetYIndex()).getRoisAsArray()[0].getYBase() - this.getManagers().get(index).getRoisAsArray()[0].getYBase());
            newProcessor.insert(transformedOriginalImage.getProcessor(), offsetXOriginal, difference);
            if (transformedImage != null) {
                newProcessor.insert(transformedImage.getProcessor(), offsetXTransformed, (this.getMaxOffsetY()));
            }
            this.addToVirtualStack(new ImagePlus("", newProcessor), this.getVirtualStack());
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
        this.setOffsetsX(new ArrayList<>());
        this.setOffsetsY(new ArrayList<>());
        this.managers = this.getManager().getRoiManagers();
        for (int index = 0; index < this.getManagers().size(); index++) {
            if (index == this.getSourceImageIndex()) {
                this.getOffsetsX().add(0);
                this.getOffsetsY().add(0);
                continue;
            }
            Roi roi = this.getManagers().get(index).getRoisAsArray()[0];
            this.getOffsetsX().add((int) (roi.getXBase() - this.getSourceImage().getManager().getRoisAsArray()[0].getXBase()));
            this.getOffsetsY().add((int) (roi.getYBase() - this.getSourceImage().getManager().getRoisAsArray()[0].getYBase()));
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
        }
    }

    private BufferedImage getSourceImage() {
        return this.sourceImage;
    }

    @SuppressWarnings("SameParameterValue")
    private void setSourceImage(boolean isWholeSlide) {
        this.sourceImage = this.getManager().getOriginal(this.getSourceImageIndex(), isWholeSlide);
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
        this.setEdgeX(-1);
        this.setEdgeY(-1);
        for (Roi points : transformedOriginalImage.getManager().getRoisAsArray()) {
            if (this.getEdgeX() == -1 || this.getEdgeX() > points.getXBase()) {
                this.setEdgeX((int) points.getXBase());
            }
            if (this.getEdgeY() == -1 || this.getEdgeY() > points.getYBase()) {
                this.setEdgeY((int) points.getYBase());
            }
        }
        this.setEdgeX2(-1);
        this.setEdgeY2(-1);
        for (Roi roi : getSourceImage().getManager().getRoisAsArray()) {
            if (this.getEdgeX2() == -1 || this.getEdgeX2() > roi.getXBase()) {
                this.setEdgeX2((int) roi.getXBase());
            }
            if (this.getEdgeY2() == -1 || this.getEdgeY2() > roi.getYBase()) {
                this.setEdgeY2((int) roi.getYBase());
            }
        }
    }
}
