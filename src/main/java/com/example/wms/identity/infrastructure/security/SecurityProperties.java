package com.example.wms.identity.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wms.security")
public record SecurityProperties(
        String jwtSecret,
        Duration tokenTtl,
        String adminUsername,
        String adminPassword) {
}
