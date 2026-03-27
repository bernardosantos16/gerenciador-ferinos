package com.bernardo.geradortimes.shared.api;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class FieldValidationException extends RuntimeException {

    private final String field;
    private final HttpStatus status;

    public FieldValidationException(HttpStatus status, String field, String message) {
        super(message);
        this.field = field;
        this.status = status;
    }

}
