package ds4h.dialog.main.event;

public class AddFileEvent implements MainDialogEvent {
    private String filePath = "";

    public AddFileEvent(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }
}
