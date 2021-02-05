package org.example.config;


import javazoom.jlgui.basicplayer.BasicPlayer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

//@EnableJpaRepositories("org.example")
@EnableScheduling
@EnableConfigurationProperties
@Configuration
@EnableAutoConfiguration
public class ApplicationConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "voice")
    public VoiceProperties getVoiceProperties() {
        return new VoiceProperties();
    }

    @Bean
    public BasicPlayer getBasicPlayer() {
        return new BasicPlayer();
    }
}
