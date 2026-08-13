package com.prostriver.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@Profile("api")
@ComponentScan(basePackages = "com.spring_ai")
@EnableMongoRepositories(basePackages = "com.spring_ai")
public class PlannerConfig {
}