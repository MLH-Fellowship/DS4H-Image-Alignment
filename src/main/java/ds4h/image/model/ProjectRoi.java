package ds4h.image.model;

import ds4h.utils.Pair;

import java.math.BigDecimal;

public class ProjectRoi {
    private Pair<BigDecimal, BigDecimal> point;
    private int roiIndex;
    private String pathFile;

    public Pair<BigDecimal, BigDecimal> getPoint() {
        return point;
    }

    public int getRoiIndex() {
        return roiIndex;
    }

    public String getPathFile() {
        return pathFile;
    }

    public void setPoint(Pair<BigDecimal, BigDecimal> point) {
        this.point = point;
    }

    public void setRoiIndex(int roiIndex) {
        this.roiIndex = roiIndex;
    }

    public void setFilePath(String pathFile) {
        this.pathFile = pathFile;
    }
}
