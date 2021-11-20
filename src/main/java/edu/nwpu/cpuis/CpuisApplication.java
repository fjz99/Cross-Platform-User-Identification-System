package edu.nwpu.cpuis;

import edu.nwpu.cpuis.utils.ApplicationInitializer;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
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
//todo delete
//todo predict
public class CpuisApplication {

    public static void main(String[] args) {
        SpringApplication.run (CpuisApplication.class, args);
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
