package edu.nwpu.cpuis.config;

import edu.nwpu.cpuis.aop.ControllerLoggingAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AutoConfigurer {
    @ConditionalOnProperty(name = "controller-logging", havingValue = "true")
    @Bean
    public ControllerLoggingAspect controllerLoggingAspect() {
        return new ControllerLoggingAspect ();
    }
}
