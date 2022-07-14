package ds4h.dialog.main.event;

public class PreviewImageEvent implements MainDialogEvent {
    private final boolean show;
    public PreviewImageEvent(boolean show) {
        this.show = show;
    }

    public boolean getValue() {
        return show;
    }
}
