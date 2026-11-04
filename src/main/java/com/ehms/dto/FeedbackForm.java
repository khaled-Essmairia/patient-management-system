package com.ehms.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FeedbackForm {
    private Long id;
    @NotNull(message = "Choose a patient") private Long patientId;
    private Long doctorId;
    @NotNull(message = "Rating is required") @Min(1) @Max(5) private Integer rating = 5;
    @Size(max = 1000) private String comment;
}
