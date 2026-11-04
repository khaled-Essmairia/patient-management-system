package com.ehms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class PatientForm {
    private Long id;

    @NotBlank(message = "First name is required") @Size(max = 100) private String firstName;
    @NotBlank(message = "Last name is required") @Size(max = 100) private String lastName;
    @NotBlank(message = "Gender is required") private String gender;

    @NotNull(message = "Date of birth is required")
    @PastOrPresent(message = "Date of birth cannot be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;

    @NotBlank(message = "Phone number is required") @Size(max = 50) private String contactNumber;
    @Size(max = 2000) private String notes;

    @Valid private List<ConsultationLine> consultations = new ArrayList<>();
    @Valid private List<AppointmentLine> appointments = new ArrayList<>();
    @Valid private List<InvoiceLine> invoices = new ArrayList<>();
}
