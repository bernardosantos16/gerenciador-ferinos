package com.bernardo.geradortimes.club.model;

import com.bernardo.geradortimes.shared.value_object.HexColor;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

@Entity
@Table(name = "clubs_jerseys")
@Getter
public class ClubJersey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private HexColor hexColor;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "is_goalkeeper_jersey", nullable = false)
    private Boolean isGoalkeeperJersey;

    @Column(name = "club_id", nullable = false)
    private UUID clubId;

    protected ClubJersey() {}

    private ClubJersey(HexColor hexColor, String name, Boolean isGoalkeeperJersey, UUID clubId) {
        this.hexColor = hexColor;
        this.name = name;
        this.isGoalkeeperJersey = isGoalkeeperJersey;
        this.clubId = clubId;
    }

    public static ClubJersey create(
            HexColor hexColor,
            String name,
            Boolean isGoalkeeperJersey,
            UUID clubId
    ) {
        return new ClubJersey(
                hexColor,
                name,
                isGoalkeeperJersey,
                clubId
         );
    }

    public void changeName(String name) {
        this.name = name;
    }

    public void changeColor(HexColor hexColor) {
        this.hexColor = hexColor;
    }

    public void changeIsGoalkeeper(Boolean isGoalkeeperJersey) {
        this.isGoalkeeperJersey = isGoalkeeperJersey;
    }
}
