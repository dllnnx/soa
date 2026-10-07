package ru.itmo.movies.repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "movie")
@Data
public class MovieEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "coord_x", nullable = false)
    private double coordinateX;

    @Column(name = "coord_y", nullable = false)
    private double coordinateY;

    @Column(name = "creation_date", nullable = false, updatable = false)
    private OffsetDateTime creationDate;

    @Column(name = "oscars_count", nullable = false)
    private long oscarsCount;

    private Integer budget;

    @Column(nullable = false)
    private String genre;

    @Column(name = "mpaa_rating", nullable = false)
    private String mpaaRating;

    @Column(name = "sw_name")
    private String screenwriterName;

    @Column(name = "sw_height")
    private Double screenwriterHeight;

    @Column(name = "sw_eye_color")
    private String screenwriterEyeColor;

    @Column(name = "sw_hair_color")
    private String screenwriterHairColor;

    @Column(name = "sw_nationality")
    private String screenwriterNationality;

    @Column(name = "sw_loc_x")
    private Float screenwriterLocationX;

    @Column(name = "sw_loc_y")
    private Integer screenwriterLocationY;

    @Column(name = "sw_loc_z")
    private Long screenwriterLocationZ;

    @PrePersist
    void initializeCreationDate() {
        if (creationDate == null) {
            creationDate = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}
