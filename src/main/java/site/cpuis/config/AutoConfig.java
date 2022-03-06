package site.cpuis.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import site.cpuis.service.DataBaseService;
import site.cpuis.service.MongoService;

@Configuration
public class AutoConfig {

    @ConditionalOnMissingBean(DataBaseService.class)
    @Bean
    @SuppressWarnings ("rawtypes")
    MongoService mongoService(MongoTemplate template) {
        return new MongoService (template);
    }

}
