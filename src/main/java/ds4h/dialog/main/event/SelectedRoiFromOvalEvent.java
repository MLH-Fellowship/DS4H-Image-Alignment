package ds4h.dialog.main.event;

public class SelectedRoiFromOvalEvent implements IMainDialogEvent {
  private final int roiIndex;
  
  public SelectedRoiFromOvalEvent(int index) {
    this.roiIndex = index;
  }
  
  public int getRoiIndex() {
    return this.roiIndex;
  }
}
