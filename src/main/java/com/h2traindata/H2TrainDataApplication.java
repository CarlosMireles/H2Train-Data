package com.h2traindata;

import com.h2traindata.infrastructure.provider.fitbit.config.FitbitProperties;
import com.h2traindata.infrastructure.provider.strava.config.StravaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({StravaProperties.class, FitbitProperties.class})
public class H2TrainDataApplication {

    public static void main(String[] args) {
        SpringApplication.run(H2TrainDataApplication.class, args);
    }
}
