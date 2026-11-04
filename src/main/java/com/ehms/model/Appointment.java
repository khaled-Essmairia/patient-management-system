package com.ehms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Getter @Setter @NoArgsConstructor
public class Appointment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Patient patient;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Doctor doctor;

    @Column(nullable = false)
    private LocalDateTime appointmentDateTime;

    private String reason;

    @Column(precision = 12, scale = 3)
    private BigDecimal fee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    private LocalDateTime createdAt = LocalDateTime.now();

    /** SMS reminder: when it was sent (null = not yet) and how many times sending was tried. */
    private LocalDateTime reminderSentAt;
    private Integer reminderAttempts = 0;
}
