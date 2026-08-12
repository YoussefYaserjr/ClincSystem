// dto/request/RefreshTokenRequest.java
package com.clinicsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {

    @NotBlank(message = "refreshToken is required")
    private String refreshToken;
}
