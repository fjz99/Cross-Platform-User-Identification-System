package site.cpuis.train.output;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class Stage3Output extends BaseOutput {
    private Map<Pair, List<Element>> output;

    @Data
    public static class Pair {
        private int id;
        private String name;
    }

    @Data
    public static class Element {
        private Pair user;
        private double similarity;
    }
}
