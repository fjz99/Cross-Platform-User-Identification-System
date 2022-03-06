package org.cpuis.train.processor;

import org.cpuis.train.TracedProcessWrapper;

/**
 * 即模型后置处理器，当完成训练后，会获得Output，这个处理器决定如何处理output
 */
public interface ModelPostProcessor {

    void process(TracedProcessWrapper processWrapper);

    boolean supports(Class<?> outputType);
}
