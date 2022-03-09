package site.cpuis.train.processor.chain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@ConfigurationProperties("chain")
@Component
@Data
public class ProcessorChainDefinition {
    private Map<String, Def> def;

    @Data
    @EqualsAndHashCode
    public static class Def {
        private Class<?> type;
        private List<String> filterNames;
        private List<Class<?>> filterTypes;
    }
}
