// dto/request/BookAppointmentRequest.java
package com.clinicsystem.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookAppointmentRequest {
    @NotNull private Long scheduleId;
    private String notes;
}