package site.cpuis.train.output;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class MatrixSimilarityOutput extends StatisticsOutput {
    private Map<Element, List<Element>> output;

    @Data
    public static class Element {
        private String name;
        private double similarity;
    }
}
