
package com.clinicsystem.dto.request;

import com.clinicsystem.entity.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.antlr.v4.runtime.misc.NotNull;

@Data
public class RegisterRequest {
    @NotBlank private String name;
    @Email @NotBlank private String email;
    @NotBlank @Size(min = 8) private String password;
    private String phone;
   // @NotNull
    private Role role; // PATIENT or DOCTOR

    // Doctor-only fields (ignored if role = PATIENT)
    private String specialty;
    private String location;

    // Patient-only fields (ignored if role = DOCTOR)
    private String bloodType;
    private String gender;
}