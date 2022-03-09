package site.cpuis.train.processor.chain;


import lombok.ToString;
import site.cpuis.train.TracedProcessWrapper;
import site.cpuis.train.processor.ModelPostProcessor;

import java.util.ArrayList;
import java.util.List;

@ToString
public class ProcessorChain {
    private final List<ModelPostProcessor> chain = new ArrayList<> ();

    public void addProcessor(ModelPostProcessor processor) {
        chain.add (processor);
    }

    public void apply(TracedProcessWrapper wrapper) {
        for (ModelPostProcessor processor : chain) {
            processor.process (wrapper);
        }
    }

}
