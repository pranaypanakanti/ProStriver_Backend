package com.kronos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KronosApplication {

    public static void main(String[] args) {
        SpringApplication.run(KronosApplication.class, args);
    }

}
