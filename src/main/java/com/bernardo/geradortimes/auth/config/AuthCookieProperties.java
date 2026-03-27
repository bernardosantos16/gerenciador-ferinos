package com.bernardo.geradortimes.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "auth.cookie")
@Setter
@Getter
public class AuthCookieProperties {

    private String refreshTokenName;
    private String path;
    private boolean secure;
    private String sameSite;

}
