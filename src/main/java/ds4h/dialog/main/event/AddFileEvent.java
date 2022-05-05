package ds4h.dialog.main.event;

public class AddFileEvent implements IMainDialogEvent {
    private String filePath = "";

    public AddFileEvent(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }
}
