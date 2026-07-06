// dto/response/ScheduleResponse.java
package com.clinicsystem.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class ScheduleResponse {
    private Long id;
    private Long doctorId;
    private LocalDate availableDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean booked;
}