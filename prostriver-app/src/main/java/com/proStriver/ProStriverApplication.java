package com.proStriver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(
        scanBasePackages = {"com.proStriver"},
        exclude = {RedisRepositoriesAutoConfiguration.class, GoogleGenAiChatAutoConfiguration.class}
)
@EnableJpaRepositories(basePackages = "com.proStriver")
@EntityScan(basePackages = {"com.proStriver", "com.springAi"})
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class ProStriverApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProStriverApplication.class, args);
    }

}

