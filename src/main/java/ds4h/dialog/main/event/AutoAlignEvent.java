package ds4h.dialog.main.event;


public class AutoAlignEvent implements MainDialogEvent, RegistrationEvent {
  private final boolean keepOriginal;
  public AutoAlignEvent(boolean keepOriginal) {
    this.keepOriginal = keepOriginal;
  }

  @Override
  public boolean isKeepOriginal() {
    return keepOriginal;
  }
}
