package com.ehms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "doctors")
@Getter @Setter @NoArgsConstructor
public class Doctor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", unique = true)
    private AppUser user;

    private String firstName;
    private String lastName;
    private String gender;
    private String contactNumber;
    private String email;
    private String qualification;
    private String doctorType;

    @Column(precision = 12, scale = 3)
    private BigDecimal consultationFee;

    public String getFullName() { return firstName + " " + lastName; }
    public String getDisplayName() { return "Dr. " + firstName + " " + lastName; }
}
