package com.bernardo.geradortimes.team.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.UUID;

@Entity
@Table(name = "teams")
@Getter
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @Column(name = "club_jersey_id")
    private Long clubJerseyId;

    protected Team() {}

    private Team(UUID matchId, Long clubJerseyId) {
        this.matchId = matchId;
        this.clubJerseyId = clubJerseyId;
    }

    public static Team create(UUID matchId, Long clubJerseyId) {
        return new Team(matchId, clubJerseyId);
    }

    public void changeJersey(Long clubJerseyId) {
        this.clubJerseyId = clubJerseyId;
    }
}
