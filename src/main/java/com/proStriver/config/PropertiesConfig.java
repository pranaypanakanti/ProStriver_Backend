package com.proStriver.config;

import com.proStriver.auth.OtpProperties;
import com.proStriver.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({SecurityProperties.class, OtpProperties.class})
public class PropertiesConfig {
}