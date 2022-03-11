package site.cpuis.train.processor.chain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import site.cpuis.train.processor.ModelPostProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public final class ProcessorChainFactory {
    private final Map<Class<?>, ProcessorChain> cache = new HashMap<> ();
    @Autowired
    private ProcessorChainDefinition definition;
    @Autowired
    private ApplicationContext context;

    public synchronized ProcessorChain getProcessor(Class<?> outputType) {
        if (cache.containsKey (outputType)) {
            return cache.get (outputType);
        }
        for (Map.Entry<String, ProcessorChainDefinition.Def> entry : definition.getDef ().entrySet ()) {
            Class<?> type = entry.getValue ().getType ();
            if (type == outputType) {
                ProcessorChain chain = new ProcessorChain ();
                List<String> names = entry.getValue ().getFilterNames ();
                List<Class<?>> types = entry.getValue ().getFilterTypes ();
                if (names != null) {
                    for (String name : names) {
                        ModelPostProcessor processor = (ModelPostProcessor) context.getBean (name);
                        chain.addProcessor (processor);
                    }
                } else if (types != null) {
                    for (Class<?> aClass : types) {
                        ModelPostProcessor processor = (ModelPostProcessor) context.getBean (aClass);
                        chain.addProcessor (processor);
                    }
                }
                log.info ("use chain {}", chain);
                cache.put (outputType, chain);
                return chain;
            }
        }
        throw new AssertionError ();
    }
}
