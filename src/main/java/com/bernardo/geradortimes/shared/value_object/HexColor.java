package com.bernardo.geradortimes.shared.value_object;

import com.bernardo.geradortimes.shared.api.FieldValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Getter
@Embeddable
public class HexColor {

    @Column(name = "hex_color", nullable = false, length = 7)
    private String value;

    protected HexColor() {
    }

    private HexColor(String value) {
        this.value = value;
    }

    public static HexColor of(String value) {
        if (value == null || value.isBlank()) {
            throw new FieldValidationException(BAD_REQUEST, "hexColor", "hexColor is required");
        }

        String raw = value.trim();
        if (raw.startsWith("#")) {
            raw = raw.substring(1);
        }

        if (raw.length() != 6) {
            throw new FieldValidationException(BAD_REQUEST, "hexColor", "hexColor must have exactly 6 hex digits");
        }

        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            boolean isHex = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F');
            if (!isHex) {
                throw new FieldValidationException(BAD_REQUEST, "hexColor", "hexColor must contain only hex digits");
            }
        }

        String normalized = "#" + raw.toUpperCase(Locale.ROOT);
        return new HexColor(normalized);
    }
}
