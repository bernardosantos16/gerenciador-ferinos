package com.bernardo.geradortimes.club.model;

import com.bernardo.geradortimes.shared.enums.ActivityStatus;
import com.bernardo.geradortimes.shared.value_object.HexColor;
import com.bernardo.geradortimes.shared.value_object.Nickname;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clubs")
@Getter
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    @Embedded
    private Nickname nickname;

    @Column(name = "created_at")
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    private ActivityStatus status;

    protected Club() {}

    private Club(String name, Nickname nickname, Instant createdAt, ActivityStatus status) {
        this.name = name;
        this.nickname = nickname;
        this.createdAt = createdAt;
        this.status = status;
    }

    public static Club create(
            String name, Nickname nickname
    ) {
        return new Club(
                name,
                nickname,
                Instant.now(),
                ActivityStatus.ACTIVE
        );
    }

    public void deactivate() {
        this.status = ActivityStatus.INACTIVE;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public void changeNickname(Nickname nickname) {
        this.nickname = nickname;
    }

}
