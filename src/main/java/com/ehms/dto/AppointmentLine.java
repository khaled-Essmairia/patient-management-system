package com.ehms.dto;

import com.ehms.model.AppointmentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
public class AppointmentLine {
    private Long id;

    @NotNull(message = "Appointment date and time are required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime appointmentDateTime;

    private Long doctorId; // ignored: the cabinet has a single doctor

    @Size(max = 255)
    private String reason;

    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @DecimalMin(value = "0", message = "Fee cannot be negative")
    private BigDecimal fee;
}
