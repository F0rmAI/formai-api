package com.formai.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FormaiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FormaiApplication.class, args);
    }
}
