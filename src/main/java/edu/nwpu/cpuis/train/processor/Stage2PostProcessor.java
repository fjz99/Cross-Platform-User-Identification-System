package edu.nwpu.cpuis.train.processor;

import edu.nwpu.cpuis.train.TracedProcessWrapper;

/**
 * @date 2021/12/31 17:28
 */
public class Stage2PostProcessor implements ModelPostProcessor {
    @Override
    public void process(TracedProcessWrapper processWrapper) {

    }

    @Override
    public boolean supports(Class<?> outputType) {
        return false;
    }
}
