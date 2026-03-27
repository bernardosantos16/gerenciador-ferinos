package com.bernardo.geradortimes.shared.security;

public interface PasswordService {
    String hash(String rawPassword);
    boolean matches(String rawPassword, String encodedHash);
}

