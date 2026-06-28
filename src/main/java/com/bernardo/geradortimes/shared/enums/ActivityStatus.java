package com.bernardo.geradortimes.shared.enums;

import lombok.Getter;

@Getter
public enum ActivityStatus {
    BANNED("user account has been banned"),
    DISABLED("user account has been disabled"),
    PENDING("user account is not verified"),
    ACTIVE(null);

    private final String errorMessage;


    ActivityStatus(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isInvalid() {
        return errorMessage != null;
    }

}
