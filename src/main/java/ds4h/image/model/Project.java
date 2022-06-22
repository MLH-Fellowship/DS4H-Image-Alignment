package ds4h.image.model;

import java.io.Serializable;
import java.util.List;

public class Project implements Serializable {
    private List<ProjectRoi> projectRois;
    private List<String> filePaths;

    public List<String> getFilePaths() {
        return this.filePaths;
    }

    public void setFilePaths(List<String> filePaths) {
        this.filePaths = filePaths;
    }

    public List<ProjectRoi> getProjectRois() {
        return this.projectRois;
    }

    public void setProjectRois(List<ProjectRoi> projectRois) {
        this.projectRois = projectRois;
    }
}
