package ds4h.image.model.project;

import ds4h.utils.Pair;

import java.math.BigDecimal;

public class ProjectImageRoi {
    private Pair<BigDecimal, BigDecimal> point;
    private int id;
    private int imageIndex;

    public Pair<BigDecimal, BigDecimal> getPoint() {
        return point;
    }

    public void setPoint(Pair<BigDecimal, BigDecimal> point) {
        this.point = point;
    }

    public int getId() {
        return id;
    }

    public void setId(int roiIndex) {
        this.id = roiIndex;
    }

    public int getImageIndex() {
        return imageIndex;
    }

    public void setImageIndex(int imageIndex) {
        this.imageIndex = imageIndex;
    }
}
