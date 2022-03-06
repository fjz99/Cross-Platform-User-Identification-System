package site.cpuis.train.output;


import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class LocationPredictOutput extends BaseOutput {
    private Map<String, String> output;
}
