package com.bernardo.geradortimes.shared.value_object;

import com.bernardo.geradortimes.shared.api.FieldValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

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
            throw new FieldValidationException(BAD_REQUEST, "nickname", "nickname is required");
        }

        String normalized = value.trim();
        if (normalized.length() > 100) {
            throw new FieldValidationException(BAD_REQUEST, "nickname", "nickname must have at most 100 characters");
        }

        return new Nickname(normalized);
    }

}
