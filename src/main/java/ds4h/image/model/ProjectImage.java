package ds4h.image.model;

import java.util.List;

public class ProjectImage {
    private final int imagesCounter;
    private final String filePath;

    private int id;

    private List<ProjectImageRoi> projectImageRois;

    public ProjectImage(int imagesCounter, String filePath) {
        this.imagesCounter = imagesCounter;
        this.filePath = filePath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getImagesCounter() {
        return imagesCounter;
    }

    public String getFilePath() {
        return filePath;
    }

    public List<ProjectImageRoi> getProjectImageRois() {
        return projectImageRois;
    }

    public void setProjectImageRois(List<ProjectImageRoi> projectImageRois) {
        this.projectImageRois = projectImageRois;
    }
}
