package com.prostriver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = {"com.prostriver"},
        exclude = {RedisRepositoriesAutoConfiguration.class, GoogleGenAiChatAutoConfiguration.class}
)
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class ProStriverApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProStriverApplication.class, args);
    }

}

