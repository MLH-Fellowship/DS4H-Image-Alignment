package ds4h.dialog.preview.event;

public class ChangeImageEvent implements IPreviewDialogEvent {
    private int index;
    public ChangeImageEvent(int index) {
        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }
}
