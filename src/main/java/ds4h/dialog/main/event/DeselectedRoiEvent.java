package ds4h.dialog.main.event;

public class DeselectedRoiEvent implements IMainDialogEvent {
  private final int roiIndex;
  
  public DeselectedRoiEvent(int index) {
    this.roiIndex = index;
  }
  
  public int getRoiIndex() {
    return this.roiIndex;
  }
  
}
