package ds4h.dialog.main.event;

public class DeleteRoiEvent implements IMainDialogEvent {
    private final int roiIndex;

    public DeleteRoiEvent(int roiIndex) {
        this.roiIndex = roiIndex;
    }

    public int getRoiIndex() {
        return this.roiIndex;
    }
}
