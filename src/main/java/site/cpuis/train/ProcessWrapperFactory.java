package site.cpuis.train;

import site.cpuis.train.output.MatrixSimilarityOutput;
import site.cpuis.train.output.NoOutputOutput;
import site.cpuis.train.output.Stage2Output;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 根据stage、phase创建对应的wrapper
 */
@Component
public final class ProcessWrapperFactory {
    @Builder
    @AllArgsConstructor
    @Data
    @NoArgsConstructor
    public final static class ProcessWrapperInput {
        private Process process;
        private String algoName;
        private String[] dataset;
        private String phase;
        private Integer thisId;
        private String directoryPath;
    }

    public TracedProcessWrapper newProcessWrapper(String stage, String phase, ProcessWrapperInput input) {
        Class<?> outputType;
        switch (stage) {
            case "1": {
                outputType = MatrixSimilarityOutput.class;
                break;
            }
            case "2": {
//                if (phase.equals ("train")) {
//                    outputType = NoOutputOutput.class;
//                } else {
//                    outputType = LocationPredictOutput.class;
//                }
                if (phase.equals ("train")) {
                    outputType = NoOutputOutput.class;
                } else {
                    outputType = Stage2Output.class;
                }
                break;
            }
            case "3":
                outputType = MatrixSimilarityOutput.class;
                break;
            default:
                throw new IllegalArgumentException ("stage" + stage);
        }
        return new TracedProcessWrapper (input.process, input.algoName,
                input.dataset, input.phase, input.thisId, input.directoryPath, outputType);
    }

}
