package com.clinicsystem.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class DoctorResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String specialty;
    private String location;
    private String clinic;
    private Integer experience;
    private BigDecimal consultationFee;
    private Double rating;
}