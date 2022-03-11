package site.cpuis.train.output;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class Stage3Output extends BaseOutput {
    private Map<String, List<Element>> output;

    @Data
    public static class Element {
//        private int id;
        private String user;
        private double similarity;
    }
}
