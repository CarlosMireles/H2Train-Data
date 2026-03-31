package com.stravatft;

import com.stravatft.config.StravaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(StravaProperties.class)
public class StravaTftApplication {

    public static void main(String[] args) {
        SpringApplication.run(StravaTftApplication.class, args);
    }
}
