package com.ehms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** An ordonnance: one medicine (with its dosage) per line of {@code content}. */
@Entity
@Table(name = "prescriptions")
@Getter @Setter @NoArgsConstructor
public class Prescription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER)
    private Doctor doctor;

    @Column(nullable = false)
    private LocalDate prescriptionDate;

    @Column(nullable = false, length = 3000)
    private String content;

    @Column(length = 1000)
    private String advice;

    private LocalDateTime createdAt = LocalDateTime.now();

    public String getReference() { return id == null ? "" : String.format("RX-%06d", id); }

    /** The non-empty lines of the content, in order. */
    public List<String> getLines() {
        List<String> out = new ArrayList<>();
        if (content != null) {
            for (String l : content.split("\\R")) {
                if (!l.isBlank()) out.add(l.trim());
            }
        }
        return out;
    }
}
