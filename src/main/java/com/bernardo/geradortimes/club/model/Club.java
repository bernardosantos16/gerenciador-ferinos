package com.bernardo.geradortimes.club.model;

import com.bernardo.geradortimes.shared.enums.ActivityStatus;
import com.bernardo.geradortimes.shared.enums.JoinPolicy;
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

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Enumerated(EnumType.STRING)
    private ActivityStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "join_policy")
    private JoinPolicy joinPolicy;

    protected Club() {}

    private Club(String name, Nickname nickname, Instant createdAt, ActivityStatus status, JoinPolicy joinPolicy) {
        this.name = name;
        this.nickname = nickname;
        this.createdAt = createdAt;
        this.status = status;
        this.joinPolicy = joinPolicy;
    }

    public static Club create(
            String name, Nickname nickname
    ) {
        return new Club(
                name,
                nickname,
                Instant.now(),
                ActivityStatus.ACTIVE,
                JoinPolicy.INVITE_ONLY
        );
    }

    public void deactivate() {
        this.status = ActivityStatus.DISABLED;
    }

    public void assignOwner(UUID userId) {
        this.ownerUserId = userId;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public void changeNickname(Nickname nickname) {
        this.nickname = nickname;
    }

    public void changeJoinPolicy(JoinPolicy joinPolicy) {
        this.joinPolicy = joinPolicy;
    }

}
