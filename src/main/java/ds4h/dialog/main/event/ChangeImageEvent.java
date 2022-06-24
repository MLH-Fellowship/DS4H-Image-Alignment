package ds4h.dialog.main.event;

public class ChangeImageEvent implements MainDialogEvent {
    private ChangeDirection direction;
    public enum ChangeDirection {
        NEXT,
        PREV
    }
    public ChangeImageEvent(ChangeDirection direction) {
        this.direction = direction;
    }

    public ChangeDirection getChangeDirection() {
        return this.direction;
    }
}
