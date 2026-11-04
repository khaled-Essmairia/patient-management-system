package com.ehms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "consultations")
@Getter @Setter @NoArgsConstructor
public class Consultation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER)
    private Doctor doctor;

    @Column(nullable = false)
    private LocalDate consultationDate;

    @Column(nullable = false)
    private String reason;

    @Column(length = 1000) private String symptoms;
    @Column(length = 1000) private String diagnosis;
    @Column(length = 1000) private String treatment;
    @Column(length = 2000) private String notes;

    private LocalDate nextVisit;
}
