package com.proStriver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@SpringBootApplication(exclude = RedisRepositoriesAutoConfiguration.class)
public class ProStriverApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProStriverApplication.class, args);
    }

}
