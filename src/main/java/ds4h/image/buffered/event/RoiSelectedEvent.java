package ds4h.image.buffered.event;

import ij.gui.Roi;

public class RoiSelectedEvent implements IBufferedImageEvent {
  private final Roi roiSelected;
  
  public RoiSelectedEvent(Roi roiSelected) {
    this.roiSelected = roiSelected;
  }
  
  public Roi getRoiSelected() {
    return this.roiSelected;
  }
}
