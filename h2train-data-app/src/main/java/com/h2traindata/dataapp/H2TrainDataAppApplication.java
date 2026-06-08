package com.h2traindata.dataapp;

import com.h2traindata.dataapp.config.DataAppDatalakeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.h2traindata")
@EnableConfigurationProperties(DataAppDatalakeProperties.class)
public class H2TrainDataAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(H2TrainDataAppApplication.class, args);
    }
}
