package com.bernardo.geradortimes.match.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "matches")
@Getter
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "club_id", nullable = false)
    private UUID clubId;

    @Column(name = "date_time", nullable = false)
    private Instant dateTime;

    protected Match() {}

    private Match(UUID clubId, Instant dateTime) {
        this.clubId = clubId;
        this.dateTime = dateTime;
    }

    public static Match create(UUID clubId, Instant dateTime) {
        return new Match(clubId, dateTime);
    }

    public void updateDateTime(Instant dateTime) {
        this.dateTime = dateTime;
    }
}
