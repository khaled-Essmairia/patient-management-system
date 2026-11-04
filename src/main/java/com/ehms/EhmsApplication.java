package com.ehms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EhmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(EhmsApplication.class, args);
    }
}
