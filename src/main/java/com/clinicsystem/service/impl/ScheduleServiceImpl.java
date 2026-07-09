package com.clinicsystem.service.impl;

import com.clinicsystem.dto.request.CreateScheduleRequest;
import com.clinicsystem.dto.response.ScheduleResponse;
import com.clinicsystem.entity.Doctor;
import com.clinicsystem.entity.Schedule;
import com.clinicsystem.exception.InvalidScheduleException;
import com.clinicsystem.exception.ResourceNotFoundException;
import com.clinicsystem.repository.DoctorRepository;
import com.clinicsystem.repository.ScheduleRepository;
import com.clinicsystem.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;

    @Override
    @Transactional
    public ScheduleResponse create(Long doctorId, CreateScheduleRequest request) {

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new InvalidScheduleException("endTime must be after startTime");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));

        List<Schedule> sameDay = scheduleRepository
                .findByDoctorIdAndAvailableDate(doctorId, request.getAvailableDate());

        boolean overlaps = sameDay.stream().anyMatch(existing ->
                request.getStartTime().isBefore(existing.getEndTime())
                        && existing.getStartTime().isBefore(request.getEndTime())
        );

        if (overlaps) {
            throw new InvalidScheduleException(
                    "This time range overlaps with an existing slot on " + request.getAvailableDate());
        }

        Schedule schedule = Schedule.builder()
                .doctor(doctor)
                .availableDate(request.getAvailableDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .booked(false)
                .build();

        schedule = scheduleRepository.save(schedule);

        return toResponse(schedule);
    }

    @Override
    public List<ScheduleResponse> getAvailableSlots(Long doctorId, LocalDate date) {
        return scheduleRepository.findByDoctorIdAndAvailableDateAndBookedFalse(doctorId, date)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long scheduleId, Long doctorId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + scheduleId));

        if (!schedule.getDoctor().getId().equals(doctorId)) {
            throw new AccessDeniedException("You do not own this schedule slot");
        }

        if (schedule.isBooked()) {
            throw new InvalidScheduleException("Cannot delete a slot that is already booked");
        }

        scheduleRepository.delete(schedule);
    }

    private ScheduleResponse toResponse(Schedule s) {
        return ScheduleResponse.builder()
                .id(s.getId())
                .doctorId(s.getDoctor().getId())
                .availableDate(s.getAvailableDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .booked(s.isBooked())
                .build();
    }
}