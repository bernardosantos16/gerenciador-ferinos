package com.bernardo.geradortimes.shared.value_object;

import com.bernardo.geradortimes.shared.api.FieldValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@Getter
@Embeddable
public class Email {

    @Column(name = "login", nullable = false, length = 100, unique = true)
    private String value;

    protected Email() {
    }

    private Email(String value) {
        this.value = value;
    }

    public static Email of(String value) {
        if (value == null || value.isBlank()) {
            log.debug("Validacao de email falhou - valor em branco");
            throw new FieldValidationException(BAD_REQUEST, "login", "login is required");
        }

        String normalized = value.trim();
        if (normalized.length() > 100) {
            log.debug("Validacao de email falhou - excede 100 caracteres");
            throw new FieldValidationException(BAD_REQUEST, "login", "login must have at most 100 characters");
        }

        return new Email(normalized);
    }

}
