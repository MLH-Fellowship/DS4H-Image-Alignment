package ds4h.dialog.main.event;

public class AlignEvent implements MainDialogEvent, RegistrationEvent {
  private final boolean keepOriginal;
  
  public AlignEvent(boolean keepOriginal) {
    this.keepOriginal = keepOriginal;
  }

  @Override
  public boolean isKeepOriginal() {
    return keepOriginal;
  }
}
