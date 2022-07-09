package ds4h.builder;

import ds4h.dialog.align.OnAlignDialogEventListener;
import ds4h.dialog.align.setting.SettingDialog;
import ds4h.dialog.loading.LoadingDialog;
import ds4h.dialog.main.event.RegistrationEvent;
import ds4h.image.buffered.BufferedImage;
import ds4h.image.manager.ImagesManager;
import ds4h.image.registration.LeastSquareImageTransformation;
import ij.IJ;
import ij.ImagePlus;
import ij.VirtualStack;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.image.ColorModel;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class LeastSquareTransformationBuilder extends AbstractBuilder<BufferedImage> {
    private static final int PROJECTIVE_MINIMUM_NUMBER = 4;
    private static final String PROJECTIVE_WARNING = "Please if you choose to use projective you need 4 Corner Points per image";
    private List<RoiManager> managers;
    private SettingDialog settingDialog;
    private BufferedImage sourceImage;


    public LeastSquareTransformationBuilder(LoadingDialog loadingDialog, ImagesManager manager, RegistrationEvent event, OnAlignDialogEventListener listener) {
        super(loadingDialog, listener, manager, event);
    }

    @Override
    public void init() {
        this.setMaximumSize(new Dimension());
        this.setSourceImage(true);
        this.setFinalStack(new Dimension(this.getMaximumSize().width, this.getMaximumSize().height));
        this.setOffsets();
        this.preInitFinalStack();
        this.checkFinalStackDimension();
        this.setFinalStackToVirtualStack();
    }

    private void preInitFinalStack() {
        this.getFinalStack().width = this.getFinalStack().width + this.getMaxOffsetX();
        this.getFinalStack().height += this.getSourceImage().getHeight() == this.getMaximumSize().height ? this.getMaxOffsetY() : 0;
    }

    @Override
    public boolean check() {
        return this.isShowSettingDialogSuccessful();
    }


    @Override
    protected ImageProcessor getFinalStackImageProcessor() {
        final ImageProcessor processor = this.getSourceImage().getProcessor().createProcessor(this.getFinalStack().width, this.getFinalStack().height);
        processor.insert(this.getSourceImage().getProcessor(), this.getMaxOffsetX(), this.getMaxOffsetY());
        return processor;
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

                if (settingDialog.getEvent().isProjective() && !this.areCornerPointsAtLeast(PROJECTIVE_MINIMUM_NUMBER)) {
                    isSuccessful[0] = false;
                    getLoadingDialog().hideDialog();
                    JOptionPane.showMessageDialog(null, PROJECTIVE_WARNING, "Warning", JOptionPane.WARNING_MESSAGE);
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            IJ.showMessage("Something is not right, sorry, contact the Developer");
        }
        return isSuccessful[0];
    }

    private boolean areCornerPointsAtLeast(int minimum) {
        return this.managers.stream().map(RoiManager::getRoisAsArray).mapToInt(roi -> roi.length).allMatch(length -> length == minimum);
    }

    @Override
    public void align() {
        this.addFinalStackToVirtualStack();
        final BufferedImage sourceImg = this.getManager().getOriginal(0, true);
        this.setVirtualStack(new VirtualStack(sourceImg.getWidth(), sourceImg.getHeight(), ColorModel.getRGBdefault(), IJ.getDir(TEMP_PATH)));
        this.addToVirtualStack(sourceImg);
        for (int index = 1; index < this.getManager().getNImages(); index++) {
            ImagePlus transformedImage = LeastSquareImageTransformation.transform(this.getManager().getOriginal(index, true), this.getManager().get(index, true), sourceImg, this.getSettingDialog().getEvent());
            if (transformedImage != null) {
                this.addToVirtualStack(transformedImage);
            }
        }
    }

    @Override
    public void alignKeepOriginal() {
        for (int index = 0; index < this.getManager().getNImages(); index++) {
            final ImageProcessor newProcessor = new ColorProcessor(this.getFinalStack().width, this.getFinalStack().height);
            final ImagePlus transformedImage = LeastSquareImageTransformation.transform(this.getManager().getOriginal(index, true), this.getManager().get(index, true), this.getSourceImage(), this.getSettingDialog().getEvent());
            final BufferedImage transformedOriginalImage = this.getManager().getOriginal(index, true);
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
            this.addToVirtualStack(new ImagePlus("", newProcessor));
        }
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
        optMaxX.ifPresent(this::setMaxOffsetX);
        this.setMaxOffsetXIndex(this.getOffsetsX().indexOf(this.getMaxOffsetX()));
        if (this.getMaxOffsetX() <= 0) {
            this.setMaxOffsetX(0);
            this.setMaxOffsetXIndex(-1);
        }
        Optional<Integer> optMaxY = this.getOffsetsY().stream().max(Comparator.naturalOrder());
        optMaxY.ifPresent(this::setMaxOffsetY);
        this.setMaxOffsetYIndex(this.getOffsetsY().indexOf(this.getMaxOffsetY()));
        if (this.getMaxOffsetY() <= 0) {
            this.setMaxOffsetY(0);
        }
    }

    @Override
    protected BufferedImage getSourceImage() {
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
}
