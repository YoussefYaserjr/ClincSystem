package com.clinicsystem.service;

import com.clinicsystem.dto.request.CreateScheduleRequest;
import com.clinicsystem.dto.response.ScheduleResponse;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleService {
    ScheduleResponse create(Long doctorId, CreateScheduleRequest request);
    List<ScheduleResponse> getAvailableSlots(Long doctorId, LocalDate date);
    void delete(Long scheduleId, Long doctorId);
}