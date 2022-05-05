package ds4h.image.buffered;


import ds4h.dialog.main.MainDialog;
import ds4h.image.buffered.event.RoiSelectedEvent;
import ds4h.utils.Utilities;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class that represents an image inside the DS4H alignment software.
 */
public class BufferedImage extends ImagePlus {
  private final RoiManager manager;
  private final boolean isReduced;
  boolean copyCornersMode;
  private Roi[] roisBackup;
  private Dimension reducedImageDimensions;
  private String filePath;
  private OnBufferedImageEventListener listener;
  private MouseListener mouseAdapter = null;
  
  public BufferedImage(String text, Image image, RoiManager manager, boolean isReduced) {
    super(text, image);
    this.manager = manager;
    this.isReduced = isReduced;
    this.buildMouseListener();
  }
  
  public BufferedImage(String text, Image image, RoiManager manager, Dimension reduceImageDimensions) {
    super(text, image);
    this.manager = manager;
    this.isReduced = true;
    this.reducedImageDimensions = reduceImageDimensions;
    this.buildMouseListener();
  }
  
  public void buildMouseListener() {
    Utilities.setTimeout(() -> {
      this.mouseAdapter = new MouseAdapter() {
        Roi startingRoi;
        Roi oldRoi;
        
        @Override
        public void mouseReleased(MouseEvent e) {
          super.mouseReleased(e);
          if (!MainDialog.currentImage.copyCornersMode) {
            Point cursorLoc = MainDialog.currentImage.getCanvas().getCursorLoc();
            Optional<Roi> roi = Arrays.stream(MainDialog.currentImage.manager.getRoisAsArray()).filter(currRoi -> currRoi.getBounds().contains(cursorLoc)).findFirst();
            if (roi.isPresent() && (roi.get() != oldRoi || !roi.get().getBounds().equals(oldRoi.getBounds()))) {
              oldRoi = roi.get();
              if (MainDialog.currentImage.listener != null) {
                MainDialog.currentImage.listener.onBufferedImageEventListener(new RoiSelectedEvent(roi.get()));
              }
            }
          } else {
            if (startingRoi == null) {
              return;
            }
            List<Roi> rois = Arrays.stream(MainDialog.currentImage.manager.getRoisAsArray()).collect(Collectors.toList());
            double movementX = MainDialog.currentImage.getRoi().getXBase() - rois.get(0).getXBase();
            double movementY = MainDialog.currentImage.getRoi().getYBase() - rois.get(0).getYBase();
            for (int i = 0; i < rois.size(); i++) {
              Roi roi = rois.get(i);
              double newLocationX = roi.getXBase() + movementX;
              double newLocationY = roi.getYBase() + movementY;
              MainDialog.currentImage.manager.select(i);
              roi.setLocation(newLocationX, newLocationY);
            }
            MainDialog.currentImage.manager.deselect();
            MainDialog.currentImage.updateAndDraw();
            MainDialog.currentImage.manager.select(Arrays.stream(MainDialog.currentImage.manager.getRoisAsArray()).collect(Collectors.toList()).indexOf(startingRoi));
            MainDialog.currentImage.copyCornersMode = false;
          }
        }
        
        @Override
        public void mousePressed(MouseEvent e) {
          super.mousePressed(e);
          if (!BufferedImage.this.copyCornersMode) {
            return;
          }
          List<Roi> rois = Arrays.stream(MainDialog.currentImage.manager.getRoisAsArray()).collect(Collectors.toList());
          for (int i = 0; i < rois.size(); i++) {
            Roi roi = rois.get(i);
            if (roi.getBounds().contains(MainDialog.currentImage.getCanvas().getCursorLoc())) {
              startingRoi = roi;
            }
          }
        }
      };
      if (this.getCanvas() != null) {
        // TODO: come smell, something is called multiple times
        this.getCanvas().addMouseListener(mouseAdapter);
      }
    }, 500);
  }
  
  public void addEventListener(OnBufferedImageEventListener listener) {
    this.listener = listener;
  }
  
  public RoiManager getManager() {
    return this.manager;
  }
  
  public void restoreRois() {
    Arrays.stream(this.roisBackup).forEach(roi -> manager.add(this, roi, 0));
  }
  
  public void backupRois() {
    this.roisBackup = this.manager.getRoisAsArray();
  }
  
  public boolean isReduced() {
    return isReduced;
  }
  
  public Dimension getEditorImageDimension() {
    return reducedImageDimensions;
  }
  
  public String getFilePath() {
    return filePath;
  }
  
  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }
  
  public void setCopyCornersMode() {
    this.copyCornersMode = true;
    IntStream.range(0, this.getManager().getRoisAsArray().length).forEachOrdered(roi -> this.getManager().select(roi, true, true));
  }
  
  public void removeMouseListeners() {
    Arrays.stream(this.getCanvas().getMouseListeners()).forEachOrdered(it -> this.getCanvas().removeMouseListener(mouseAdapter));
  }
}