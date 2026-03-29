package com.ProStriver.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "prostriver.cors")
public class CorsProperties {
    private List<String> allowedOrigins = new ArrayList<>();
}