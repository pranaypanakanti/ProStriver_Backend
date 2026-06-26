package com.springAi.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public DefaultErrorHandler errorHandler() {
        ExponentialBackOffWithMaxRetries backoff = new ExponentialBackOffWithMaxRetries(2);
        backoff.setInitialInterval(2_000);
        backoff.setMultiplier(2.0);
        backoff.setMaxInterval(10_000);

        DefaultErrorHandler handler = new DefaultErrorHandler(backoff);

        handler.addNotRetryableExceptions(
                JsonProcessingException.class,
                IllegalArgumentException.class
        );

        return handler;
    }
}