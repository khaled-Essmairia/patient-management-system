package com.ehms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReportForm {
    @NotBlank(message = "Diagnosis is required") @Size(max = 1000) private String diagnosis;
    @Size(max = 1000) private String medicinePrescribed;
    @Size(max = 2000) private String doctorComment;
}
