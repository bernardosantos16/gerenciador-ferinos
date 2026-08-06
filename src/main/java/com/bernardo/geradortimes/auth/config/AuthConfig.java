package com.bernardo.geradortimes.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, AuthCookieProperties.class, CorsProperties.class, RateLimitProperties.class})
public class AuthConfig {
}
