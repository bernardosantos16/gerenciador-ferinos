package com.bernardo.geradortimes.club.model;

import com.bernardo.geradortimes.shared.enums.MembershipRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "club_membership_requests")
@Getter
public class ClubMembershipRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_id", nullable = false)
    private UUID clubId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 250)
    private String name;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipRequestStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    protected ClubMembershipRequest() {}

    private ClubMembershipRequest(UUID clubId, UUID userId, String name, String nickname) {
        this.clubId = clubId;
        this.userId = userId;
        this.name = name;
        this.nickname = nickname;
        this.status = MembershipRequestStatus.PENDING;
        this.requestedAt = Instant.now();
    }

    public static ClubMembershipRequest create(UUID clubId, UUID userId, String name, String nickname) {
        return new ClubMembershipRequest(clubId, userId, name, nickname);
    }

    public boolean isPending() {
        return status == MembershipRequestStatus.PENDING;
    }

    public void approve(UUID directorId) {
        this.status = MembershipRequestStatus.APPROVED;
        this.reviewedAt = Instant.now();
        this.reviewedBy = directorId;
    }

    public void reject(UUID directorId) {
        this.status = MembershipRequestStatus.REJECTED;
        this.reviewedAt = Instant.now();
        this.reviewedBy = directorId;
    }
}
