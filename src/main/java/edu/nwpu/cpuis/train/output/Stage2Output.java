package edu.nwpu.cpuis.train.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Stage2Output {
    String name;
    List<String> content;
    List<String> originalLabel;
    List<String> predictedLabel;
}
