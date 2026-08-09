package com.clinicsystem.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "doctors")
@PrimaryKeyJoinColumn(name = "id")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
public class Doctor extends User {

    @Column(nullable = false)
    private String specialty;

    @Column(nullable = false)
    private String location; // clinic address / area — used for search

    private String clinic;

    private Integer experience; // years

    @Column(precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Column(nullable = false)
    @Builder.Default
    private Double rating = 0.0;

    /** Admin approval gate. Only approved doctors appear in public discovery. */
    @Column(nullable = false)
    @Builder.Default
    private boolean approved = false;
}