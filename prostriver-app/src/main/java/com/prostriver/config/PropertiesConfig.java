package com.prostriver.config;

import com.prostriver.auth.OtpProperties;
import com.prostriver.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({SecurityProperties.class, OtpProperties.class})
public class PropertiesConfig {
}