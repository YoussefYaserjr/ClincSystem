// dto/response/AuthResponse.java
package com.clinicsystem.dto.response;

import lombok.*;

@Data @Builder
public class AuthResponse {
    private String token;
    private Long userId;
    private String role;
}