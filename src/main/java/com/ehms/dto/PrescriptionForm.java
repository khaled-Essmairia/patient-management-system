package com.ehms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter @Setter
public class PrescriptionForm {
    private Long id;

    @NotNull(message = "Choose a patient")
    private Long patientId;

    @NotNull(message = "Prescription date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate prescriptionDate;

    @NotBlank(message = "Write at least one medicine")
    @Size(max = 3000)
    private String content;

    @Size(max = 1000)
    private String advice;
}
