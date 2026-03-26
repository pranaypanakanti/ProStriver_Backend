package com.ProStriver;

import com.ProStriver.security.SecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
public class ProStriverApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProStriverApplication.class, args);
    }

}
