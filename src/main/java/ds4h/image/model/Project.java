package ds4h.image.model;

import java.io.Serializable;
import java.util.List;

public class Project implements Serializable {
    private List<ProjectImage> projectImages;

    public List<ProjectImage> getProjectImages() {
        return projectImages;
    }

    public void setProjectImages(List<ProjectImage> projectImages) {
        this.projectImages = projectImages;
    }
}
