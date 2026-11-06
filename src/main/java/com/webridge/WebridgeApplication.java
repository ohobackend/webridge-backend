package com.webridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WebridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebridgeApplication.class, args);
    }
}
