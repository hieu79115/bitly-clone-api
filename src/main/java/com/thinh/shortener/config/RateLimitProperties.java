package com.thinh.shortener.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private Tier auth = new Tier(10, 60);
    private Tier redirect = new Tier(100, 60);
    private Tier createUrl = new Tier(20, 60);
    private Tier general = new Tier(120, 60);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tier {
        private int capacity;
        private int durationSeconds;
    }
}
