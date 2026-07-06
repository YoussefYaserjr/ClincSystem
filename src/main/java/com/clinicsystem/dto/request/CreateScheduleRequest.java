// dto/request/CreateScheduleRequest.java
package com.clinicsystem.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CreateScheduleRequest {
    @NotNull private LocalDate availableDate;
    @NotNull private LocalTime startTime;
    @NotNull private LocalTime endTime;
}