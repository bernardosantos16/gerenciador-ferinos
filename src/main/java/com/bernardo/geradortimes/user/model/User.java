package com.bernardo.geradortimes.user.model;

import com.bernardo.geradortimes.shared.enums.ActivityStatus;
import com.bernardo.geradortimes.shared.enums.UserRole;
import com.bernardo.geradortimes.shared.value_object.Email;
import com.bernardo.geradortimes.shared.value_object.Nickname;
import com.bernardo.geradortimes.shared.value_object.PasswordHash;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
public class User {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private String name;

    @Embedded
    private Nickname nickname;

    @Embedded
    private Email login;

    @Embedded
    private PasswordHash password;

    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    private ActivityStatus status;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    protected User() {
    }

    private User(String name, Nickname nickname, Email email, PasswordHash password, Instant createdAt, ActivityStatus activityStatus, UserRole role) {
        this.name = name;
        this.nickname = nickname;
        this.login = email;
        this.password = password;
        this.createdAt = createdAt;
        this.status = activityStatus;
        this.role = role;
    }

    public static User create(String name, Nickname nickname, Email email, PasswordHash password) {
        return new User(
                name,
                nickname,
                email,
                password,
                Instant.now(),
                ActivityStatus.PENDING,
                UserRole.USER
        );
    }

    public void activateUser() {
        this.status = ActivityStatus.ACTIVE;
    }

    public void inactivateUser() {
        this.status = ActivityStatus.INACTIVE;
    }

}
