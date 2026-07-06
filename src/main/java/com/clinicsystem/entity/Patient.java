package com.clinicsystem.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "patients")
@PrimaryKeyJoinColumn(name = "id")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
public class Patient extends User {

    private String bloodType;
    private String gender;
    private LocalDate dateOfBirth;
}