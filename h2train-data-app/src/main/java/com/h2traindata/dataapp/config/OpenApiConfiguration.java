package com.h2traindata.dataapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI h2TrainDataOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("H2Train Data API")
                        .version("v1")
                        .description("API de solo lectura para datos longitudinales y datasets bajo demanda."));
    }
}
