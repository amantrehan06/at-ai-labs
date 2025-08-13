package com.executor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main Spring Boot Application
 * 
 * This is the entry point for the AT AI Labs application.
 * It scans all modules to include their controllers and components.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.executor",
    "com.common",
    "com.codeassistant",
    "com.documentrag"
})
public class ATAILabsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ATAILabsApplication.class, args);
    }
} 