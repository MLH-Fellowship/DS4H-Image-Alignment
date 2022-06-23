package ds4h.dialog.main.event;


public class AutoAlignEvent implements IMainDialogEvent {
  private final boolean keepOriginal;
  public AutoAlignEvent(boolean keepOriginal) {
    this.keepOriginal = keepOriginal;
  }

  public boolean isKeepOriginal() {
    return keepOriginal;
  }
}
