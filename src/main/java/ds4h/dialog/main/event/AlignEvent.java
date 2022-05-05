package ds4h.dialog.main.event;

public class AlignEvent implements IMainDialogEvent {
  private final boolean keepOriginal;
  
  public AlignEvent(boolean keepOriginal) {
    this.keepOriginal = keepOriginal;
  }
  
  public boolean isKeepOriginal() {
    return keepOriginal;
  }
}
