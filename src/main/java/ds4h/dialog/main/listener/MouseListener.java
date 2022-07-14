package ds4h.dialog.main.listener;

import ds4h.dialog.main.MainDialog;
import ds4h.image.model.manager.slide.event.RoiSelectedEvent;
import ij.gui.Roi;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MouseListener extends MouseAdapter {
    private final MainDialog dialog;
    private Roi startingRoi;
    private Roi oldRoi;

    public MouseListener(MainDialog dialog) {
        this.dialog = dialog;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        super.mouseReleased(e);
        if (!this.getMainDialog().getCurrentImage().isCopyCornersMode()) {
            Point cursorLoc = this.getMainDialog().getCurrentImage().getCanvas().getCursorLoc();
            Optional<Roi> roi = Arrays.stream(this.getMainDialog().getCurrentImage().getManager().getRoisAsArray()).filter(currRoi -> currRoi.getBounds().contains(cursorLoc)).findFirst();
            if (roi.isPresent() && (roi.get() != oldRoi || !roi.get().getBounds().equals(oldRoi.getBounds()))) {
                oldRoi = roi.get();
                if (this.getMainDialog().getCurrentImage().getListener() != null) {
                    this.getMainDialog().getCurrentImage().getListener().onBufferedImageEventListener(new RoiSelectedEvent(roi.get()));
                }
            }
        } else {
            if (startingRoi == null) {
                return;
            }
            List<Roi> rois = Arrays.stream(this.getMainDialog().getCurrentImage().getManager().getRoisAsArray()).collect(Collectors.toList());
            double movementX = this.getMainDialog().getCurrentImage().getRoi().getXBase() - rois.get(0).getXBase();
            double movementY = this.getMainDialog().getCurrentImage().getRoi().getYBase() - rois.get(0).getYBase();
            for (int i = 0; i < rois.size(); i++) {
                Roi roi = rois.get(i);
                double newLocationX = roi.getXBase() + movementX;
                double newLocationY = roi.getYBase() + movementY;
                this.getMainDialog().getCurrentImage().getManager().select(i);
                roi.setLocation(newLocationX, newLocationY);
            }
            this.getMainDialog().getCurrentImage().getManager().deselect();
            this.getMainDialog().getCurrentImage().updateAndDraw();
            this.getMainDialog().getCurrentImage().getManager().select(Arrays.stream(this.getMainDialog().getCurrentImage().getManager().getRoisAsArray()).collect(Collectors.toList()).indexOf(startingRoi));
            this.getMainDialog().getCurrentImage().setCopyCornersMode(false);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        if (!getMainDialog().getCurrentImage().isCopyCornersMode()) {
            return;
        }
        List<Roi> rois = Arrays.stream(getMainDialog().getCurrentImage().getManager().getRoisAsArray()).collect(Collectors.toList());
        for (Roi roi : rois) {
            if (roi.getBounds().contains(getMainDialog().getCurrentImage().getCanvas().getCursorLoc())) {
                startingRoi = roi;
            }
        }
    }


    private MainDialog getMainDialog() {
        return dialog;
    }
}
