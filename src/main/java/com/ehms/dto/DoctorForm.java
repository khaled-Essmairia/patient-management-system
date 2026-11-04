package com.ehms.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class DoctorForm {
    private Long id;
    @NotBlank(message = "Username is required") private String username;
    private String password; // required on creation, optional on update
    @NotBlank(message = "First name is required") private String firstName;
    @NotBlank(message = "Last name is required") private String lastName;
    @NotBlank(message = "Gender is required") private String gender;
    private String contactNumber;
    @Email(message = "Email is not valid") private String email;
    @NotBlank(message = "Qualification is required") private String qualification;
    @NotBlank(message = "Specialty is required") private String doctorType;
    @NotNull(message = "Consultation fee is required") @DecimalMin("0") private BigDecimal consultationFee;
}
