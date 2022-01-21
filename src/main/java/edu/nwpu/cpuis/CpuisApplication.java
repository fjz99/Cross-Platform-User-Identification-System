package edu.nwpu.cpuis;

import edu.nwpu.cpuis.utils.ApplicationInitializer;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@SpringBootApplication
@EnableCaching
@EnableAsync
@PropertySource("classpath:models.properties")
@EnableScheduling
//@EnablePrometheusEndpoint
//@EnableSpringBootMetricsCollector
public class CpuisApplication {

    public static void main(String[] args) {
        SpringApplication.run (CpuisApplication.class, args);
    }

    @Bean
    MeterRegistryCustomizer<MeterRegistry> configurer(
            @Value("${spring.application.name}") String applicationName) {
        return (registry) -> registry.config ().commonTags ("application", applicationName);
    }

    @Component
    public static class App implements ApplicationRunner {
        @Resource
        private ApplicationInitializer initializer;

        @Override
        public void run(ApplicationArguments args) {
            initializer.init ();
        }
    }

}
