package ds4h.image.model.manager.slide.event;

import ij.gui.Roi;

public class RoiSelectedEvent implements SlideImageEvent {
  private final Roi roiSelected;
  
  public RoiSelectedEvent(Roi roiSelected) {
    this.roiSelected = roiSelected;
  }
  
  public Roi getRoiSelected() {
    return this.roiSelected;
  }
}
