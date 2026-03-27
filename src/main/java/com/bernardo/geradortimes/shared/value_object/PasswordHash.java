package com.bernardo.geradortimes.shared.value_object;

import com.bernardo.geradortimes.shared.api.FieldValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Getter
@Embeddable
public class PasswordHash {

    @Column(name = "password", nullable = false, length = 255)
    private String value;

    protected PasswordHash() {
    }

    private PasswordHash(String value) {
        this.value = value;
    }

    public static PasswordHash fromEncoded(String value) {
        if (value == null || value.isBlank()) {
            throw new FieldValidationException(BAD_REQUEST, "password", "password hash is required");
        }

        String normalized = value.trim();
        if (normalized.length() > 255) {
            throw new FieldValidationException(BAD_REQUEST, "password", "password hash must have at most 255 characters");
        }

        return new PasswordHash(normalized);
    }

}
