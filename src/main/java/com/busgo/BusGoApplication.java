package com.busgo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BusGoApplication {
    public static void main(String[] args) {
        SpringApplication.run(BusGoApplication.class, args);
    }
}
