package ds4h.dialog.preview.event;

public class ChangeImagePreviewEvent implements IPreviewDialogEvent {
    private final int index;
    public ChangeImagePreviewEvent(int index) {
        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }
}
