package edu.nwpu.cpuis.train.processor;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Data
public final class ProcessorFactory {
    private final List<ModelPostProcessor> list;

    public ProcessorFactory(List<ModelPostProcessor> list) {
        this.list = list;
    }


    public ModelPostProcessor getProcessor(Class<?> outputType) {
        for (ModelPostProcessor modelPostProcessor : list) {
            if (modelPostProcessor.supports (outputType)) {
                return modelPostProcessor;
            }
        }
        throw new AssertionError ();
    }
}
