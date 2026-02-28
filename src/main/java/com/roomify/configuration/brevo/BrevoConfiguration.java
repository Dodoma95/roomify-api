package com.roomify.configuration.brevo;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(BrevoProperties.class)
@Configuration
public class BrevoConfiguration {
}
