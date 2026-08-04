// dto/request/CreateMedicalRecordRequest.java
package com.clinicsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateMedicalRecordRequest {
    @NotNull private Long patientId;
    private Long appointmentId; // required: record must be linked to a visit

    @NotBlank private String diagnosis;
    private String prescription;
    private String notes;
}