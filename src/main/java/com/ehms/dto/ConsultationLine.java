package com.ehms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** One consultation (one visit). Used as a grid line in the patient file and as base of the standalone form. */
@Getter @Setter
public class ConsultationLine {
    private Long id;

    @NotNull(message = "Consultation date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate consultationDate;

    private Long doctorId;

    @NotBlank(message = "Consultation reason is required")
    @Size(max = 255)
    private String reason;

    @Size(max = 1000) private String symptoms;
    @Size(max = 1000) private String diagnosis;
    @Size(max = 1000) private String treatment;
    @Size(max = 2000) private String notes;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate nextVisit;
}
