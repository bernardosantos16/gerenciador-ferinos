package com.bernardo.geradortimes.auth.service;

import com.bernardo.geradortimes.auth.config.AuthCookieProperties;
import com.bernardo.geradortimes.auth.config.JwtProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class AuthCookieService {

    private final AuthCookieProperties cookieProperties;
    private final JwtProperties jwtProperties;

    public AuthCookieService(AuthCookieProperties cookieProperties, JwtProperties jwtProperties) {
        this.cookieProperties = cookieProperties;
        this.jwtProperties = jwtProperties;
    }

    public String refreshTokenCookieName() {
        return cookieProperties.getRefreshTokenName();
    }

    public ResponseCookie buildRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(cookieProperties.getRefreshTokenName(), refreshToken)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .path(cookieProperties.getPath())
                .maxAge(jwtProperties.refreshTokenTtl())
                .sameSite(cookieProperties.getSameSite())
                .build();
    }

    public ResponseCookie clearRefreshTokenCookie() {
        return ResponseCookie.from(cookieProperties.getRefreshTokenName(), "")
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .path(cookieProperties.getPath())
                .maxAge(0)
                .sameSite(cookieProperties.getSameSite())
                .build();
    }
}
