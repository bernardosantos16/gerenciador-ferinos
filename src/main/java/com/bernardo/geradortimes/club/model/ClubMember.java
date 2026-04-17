package com.bernardo.geradortimes.club.model;

import com.bernardo.geradortimes.shared.enums.ClubRole;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;


@Entity
@Table(name = "clubs_members")
@Getter
public class ClubMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "club_id")
    private UUID clubId;

    private String name;

    private Integer rating;

    @Column(name = "times_mvp")
    private Integer timesMvp;

    @Column(name = "times_champion")
    private Integer timesChampion;

    @Column(name = "team_id")
    private Long teamId;

    @Enumerated(EnumType.STRING)
    @Column(name = "club_role")
    private ClubRole clubRole;

    protected ClubMember() {}

    private ClubMember(
            UUID userId,
            UUID clubId,
            String name,
            Integer rating,
            Integer timesMvp,
            Integer timesChampion,
            Long teamId,
            ClubRole clubRole
    ) {
        this.userId = userId;
        this.clubId = clubId;
        this.name = name;
        this.rating = rating;
        this.timesMvp = timesMvp;
        this.timesChampion = timesChampion;
        this.teamId = teamId;
        this.clubRole = clubRole;
    }

    public static ClubMember create(
            UUID userId,
            UUID clubId,
            String name,
            Integer rating,
            Integer timesMvp,
            Integer timesChampion,
            Long teamId,
            ClubRole clubRole
    ){
        return new ClubMember(
                userId,
                clubId,
                name,
                rating,
                timesMvp,
                timesChampion,
                teamId,
                clubRole
        );
    }

    public void changeName(String name) {
        this.name = name;
    }

    public void changeRating(Integer rating) {
        this.rating = rating;
    }

    public void changeRole(ClubRole role) {
        this.clubRole = role;
    }
}
