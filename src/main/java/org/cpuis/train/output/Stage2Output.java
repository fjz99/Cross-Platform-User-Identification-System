package org.cpuis.train.output;

import lombok.*;

import java.util.List;


@Getter
@Setter
public class Stage2Output extends BaseOutput {
    private List<Element> output;

    @Data
    private static class Element {
        private String input;
        private String predict;
        private List<Double> degree;
    }
}
