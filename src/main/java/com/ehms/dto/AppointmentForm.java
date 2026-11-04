package com.ehms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AppointmentForm extends AppointmentLine {
    @NotNull(message = "Choose a patient")
    private Long patientId;
}
