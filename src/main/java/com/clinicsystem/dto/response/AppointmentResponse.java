// dto/response/AppointmentResponse.java
package com.clinicsystem.dto.response;

import com.clinicsystem.entity.enums.AppointmentStatus;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder
public class AppointmentResponse {
    private Long id;
    private Long doctorId;
    private String doctorName;
    private Long patientId;
    private String patientName;
    private LocalDateTime appointmentTime;
    private AppointmentStatus status;
}