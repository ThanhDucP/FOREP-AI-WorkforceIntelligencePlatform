package com.aiworkforce.platform.config.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Workforce Intelligence Platform API")
                        .version("1.0")
                        .description("Platform for workflow analytics and AI-driven organizational insights.")
                        .contact(new Contact()
                                .name("Engineering Team")
                                .email("eng@aiworkforce.com")));
    }
}
