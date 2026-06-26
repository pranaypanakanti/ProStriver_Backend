package com.proStriver.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@Profile("api")
@ComponentScan(basePackages = "com.springAi")
@EnableMongoRepositories(basePackages = "com.springAi")
public class PlannerConfig {
}