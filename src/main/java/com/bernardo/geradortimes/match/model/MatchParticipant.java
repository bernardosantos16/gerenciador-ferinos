package com.bernardo.geradortimes.match.model;

import com.bernardo.geradortimes.shared.enums.MatchParticipantPosition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.UUID;

@Entity
@Table(name = "matches_participants")
@Getter
public class MatchParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @Column(name = "club_member_id", nullable = false)
    private Long clubMemberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchParticipantPosition position;

    @Column(name = "team_id")
    private Long teamId;

    protected MatchParticipant() {}

    private MatchParticipant(UUID matchId, Long clubMemberId, MatchParticipantPosition position, Long teamId) {
        this.matchId = matchId;
        this.clubMemberId = clubMemberId;
        this.position = position;
        this.teamId = teamId;
    }

    public static MatchParticipant create(UUID matchId, Long clubMemberId, MatchParticipantPosition position, Long teamId) {
        return new MatchParticipant(matchId, clubMemberId, position, teamId);
    }

    public void assignTeam(Long teamId) {
        this.teamId = teamId;
    }
}

