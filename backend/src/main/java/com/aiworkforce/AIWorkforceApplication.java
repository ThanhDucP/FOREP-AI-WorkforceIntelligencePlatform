package com.aiworkforce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AIWorkforceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AIWorkforceApplication.class, args);
    }
}
