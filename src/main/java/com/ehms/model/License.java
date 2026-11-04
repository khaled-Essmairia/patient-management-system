package com.ehms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A license key entered in License Management. The newest row is the active license. */
@Entity
@Table(name = "licenses")
@Getter @Setter @NoArgsConstructor
public class License {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "license_key", length = 1000)
    private String licenseKey;

    @Column(name = "customer")
    private String customer;

    @Column(name = "expires_on")
    private LocalDate expiresOn;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;
}
