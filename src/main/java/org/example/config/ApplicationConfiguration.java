package org.example.config;


import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaRepositories("org.example")
@EnableScheduling
@EnableConfigurationProperties
@Configuration
@EnableAutoConfiguration
public class ApplicationConfiguration {

}
