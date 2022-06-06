package ds4h.image.model;

import ds4h.utils.Pair;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class Project implements Serializable {
    private Map<Pair<BigDecimal, BigDecimal>, Integer> imagesIndexesWithRois;
    private List<String> filePaths;

    public Project() {}

    public List<String> getFilePaths() {
        return this.filePaths;
    }

    public Map<Pair<BigDecimal, BigDecimal>, Integer> getImagesIndexesWithRois() {
        return this.imagesIndexesWithRois;
    }

    public void setFilePaths(List<String> filePaths) {
        this.filePaths = filePaths;
    }

    public void setImagesIndexesWithRois(Map<Pair<BigDecimal, BigDecimal>, Integer> imagesIndexesWithRois) {
        this.imagesIndexesWithRois = imagesIndexesWithRois;
    }
}
