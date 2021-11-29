package edu.nwpu.cpuis.train.processor;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Data
public final class ProcessorFactory {
    private final Map<String, ModelPostProcessor> map;

    public ProcessorFactory(Map<String, ModelPostProcessor> map) {
        this.map = map;
    }

    //简单工厂模式
    public ModelPostProcessor getProcessor(String phase, int stage) {
        switch (stage) {
            case 1: {
                return map.get ("matrixValuePostProcessor");
            }
            case 2: {
                if (phase.equals ("train")) {
                    return map.get ("doNothingPostProcessor");
                } else return null;
            }
            case 3: {
                return null;
            }
            default:
                throw new IllegalArgumentException ();
        }
    }
}
