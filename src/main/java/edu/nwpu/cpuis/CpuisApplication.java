package edu.nwpu.cpuis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EnableAsync
@PropertySource ("classpath:models.properties")
public class CpuisApplication {

    public static void main(String[] args) {
        SpringApplication.run (CpuisApplication.class, args);
    }

}
