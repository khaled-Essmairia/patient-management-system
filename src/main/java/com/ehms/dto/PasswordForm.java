package com.ehms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PasswordForm {
    @NotBlank(message = "Enter your old password") private String oldPassword;
    @NotBlank(message = "Enter a new password") private String newPassword;
    @NotBlank(message = "Confirm the new password") private String confirmPassword;
}
