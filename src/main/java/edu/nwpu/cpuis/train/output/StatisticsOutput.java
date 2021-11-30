package edu.nwpu.cpuis.train.output;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class StatisticsOutput extends BaseOutput {
    private Map<String, String> statistics;
}
