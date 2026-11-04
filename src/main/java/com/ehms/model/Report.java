package com.ehms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Getter @Setter @NoArgsConstructor
public class Report {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(unique = true)
    private Appointment appointment;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Patient patient;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Doctor doctor;

    @Column(length = 1000) private String diagnosis;
    @Column(length = 1000) private String medicinePrescribed;
    @Column(length = 2000) private String doctorComment;

    private LocalDateTime createdAt = LocalDateTime.now();
}
