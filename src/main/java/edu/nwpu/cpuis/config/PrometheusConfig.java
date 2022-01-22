package edu.nwpu.cpuis.config;


import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置micrometer
 *
 * @date 2022/1/22 10:49
 */
@Configuration("myPrometheusConfig")
public class PrometheusConfig {

    /**
     * springboot自动配置了默认的metrics
     */
    @Bean
    MeterRegistryCustomizer<MeterRegistry> configurer(
            @Value("${spring.application.name}") String applicationName) {
        return (registry) -> {
            registry.config ().commonTags ("application", applicationName);
        };
    }

}
