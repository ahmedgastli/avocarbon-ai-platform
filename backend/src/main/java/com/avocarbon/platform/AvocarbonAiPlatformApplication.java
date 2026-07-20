package com.avocarbon.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AvocarbonAiPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AvocarbonAiPlatformApplication.class, args);
    }

}
