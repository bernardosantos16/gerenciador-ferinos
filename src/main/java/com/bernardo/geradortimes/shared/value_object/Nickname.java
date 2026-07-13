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
public class Nickname {

    @Column(name = "nickname", nullable = false, length = 100, unique = true)
    private String value;

    protected Nickname() {
    }

    private Nickname(String value) {
        this.value = value;
    }

    public static Nickname of(String value) {
        if (value == null || value.isBlank()) {
            log.debug("Validacao de nickname falhou - valor em branco");
            throw new FieldValidationException(BAD_REQUEST, "nickname", "nickname is required");
        }

        String normalized = value.trim();
        if (normalized.length() > 100) {
            log.debug("Validacao de nickname falhou - excede 100 caracteres");
            throw new FieldValidationException(BAD_REQUEST, "nickname", "nickname must have at most 100 characters");
        }

        return new Nickname(normalized);
    }

}
