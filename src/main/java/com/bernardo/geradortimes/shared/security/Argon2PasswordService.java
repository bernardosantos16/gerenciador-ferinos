package com.bernardo.geradortimes.shared.security;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class Argon2PasswordService implements PasswordService {

    @Value("${argon.hash.pepper}")
    private String argonPepper;

    @Override
    public String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("password is required");
        }

        Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id, 46, 46);
        String pepperPassword = rawPassword.concat(argonPepper);
        char[] passwordChars = pepperPassword.toCharArray();
        try {
            // Baseline parameters. Tune as needed.
            return argon2.hash(3, 1 << 16, 4, passwordChars);
        } finally {
            argon2.wipeArray(passwordChars);
        }
    }

    @Override
    public boolean matches(String rawPassword, String encodedHash) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return false;
        }
        if (encodedHash == null || encodedHash.isBlank()) {
            return false;
        }

        Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id, 46, 46);
        String pepperPassword = rawPassword.concat(argonPepper);
        char[] passwordChars = pepperPassword.toCharArray();
        try {
            return argon2.verify(encodedHash, passwordChars);
        } finally {
            argon2.wipeArray(passwordChars);
        }
    }
}
