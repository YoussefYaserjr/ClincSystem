package com.clinicsystem.dto.request;

import com.clinicsystem.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 8, message = "password must be at least 8 characters")
    private String password;

    private String phone;

    /** Only PATIENT or DOCTOR are allowed at registration. ADMIN is not self-service. */
    @NotNull(message = "role is required (PATIENT or DOCTOR)")
    private Role role;

    // Doctor-only fields (validated in AuthService when role = DOCTOR)
    private String specialty;
    private String location;

    // Patient-only fields (optional)
    private String bloodType;
    private String gender;
}
