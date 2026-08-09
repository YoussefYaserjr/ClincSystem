package com.clinicsystem.service;

import com.clinicsystem.dto.request.CreateScheduleRequest;
import com.clinicsystem.dto.response.ScheduleResponse;
import com.clinicsystem.entity.Doctor;
import com.clinicsystem.entity.Schedule;
import com.clinicsystem.exception.InvalidScheduleException;
import com.clinicsystem.exception.ResourceNotFoundException;
import com.clinicsystem.repository.DoctorRepository;
import com.clinicsystem.repository.ScheduleRepository;
import com.clinicsystem.service.impl.ScheduleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ScheduleServiceImplTest {

    private ScheduleRepository scheduleRepository;
    private DoctorRepository doctorRepository;
    private ScheduleServiceImpl service;

    @BeforeEach
    void setUp() {
        scheduleRepository = mock(ScheduleRepository.class);
        doctorRepository = mock(DoctorRepository.class);
        service = new ScheduleServiceImpl(scheduleRepository, doctorRepository);
    }

    @Test
    void createValidSlotReturnsResponse() {
        Doctor doctor = Doctor.builder().id(1L).name("Dr. Smith").build();
        LocalDate date = LocalDate.now().plusDays(1);
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(scheduleRepository.findByDoctorIdAndAvailableDate(1L, date)).thenReturn(List.of());
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> {
            Schedule s = inv.getArgument(0);
            s.setId(7L);
            return s;
        });

        CreateScheduleRequest request = new CreateScheduleRequest();
        request.setAvailableDate(date);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));

        ScheduleResponse response = service.create(1L, request);

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getDoctorId()).isEqualTo(1L);
        assertThat(response.isBooked()).isFalse();
    }

    @Test
    void overlappingSlotIsRejected() {
        Doctor doctor = Doctor.builder().id(1L).build();
        LocalDate date = LocalDate.now().plusDays(1);
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        Schedule existing = Schedule.builder().id(1L).availableDate(date)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0)).build();
        when(scheduleRepository.findByDoctorIdAndAvailableDate(1L, date)).thenReturn(List.of(existing));

        CreateScheduleRequest request = new CreateScheduleRequest();
        request.setAvailableDate(date);
        request.setStartTime(LocalTime.of(9, 30));
        request.setEndTime(LocalTime.of(10, 30));

        assertThatThrownBy(() -> service.create(1L, request))
                .isInstanceOf(InvalidScheduleException.class);
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void endTimeBeforeStartTimeIsRejected() {
        CreateScheduleRequest request = new CreateScheduleRequest();
        request.setAvailableDate(LocalDate.now().plusDays(1));
        request.setStartTime(LocalTime.of(10, 0));
        request.setEndTime(LocalTime.of(9, 0));

        assertThatThrownBy(() -> service.create(1L, request))
                .isInstanceOf(InvalidScheduleException.class);
        verify(doctorRepository, never()).findById(any());
    }

    @Test
    void deleteSlotBelongingToAnotherDoctorIsForbidden() {
        Doctor owner = Doctor.builder().id(1L).build();
        Schedule schedule = Schedule.builder().id(5L).doctor(owner).booked(false).build();
        when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> service.delete(5L, 99L))
                .isInstanceOf(AccessDeniedException.class);
        verify(scheduleRepository, never()).delete(any());
    }

    @Test
    void deleteBookedSlotIsRejected() {
        Doctor owner = Doctor.builder().id(1L).build();
        Schedule schedule = Schedule.builder().id(5L).doctor(owner).booked(true).build();
        when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> service.delete(5L, 1L))
                .isInstanceOf(InvalidScheduleException.class);
        verify(scheduleRepository, never()).delete(any());
    }

    @Test
    void deleteOwnFreeSlotSucceeds() {
        Doctor owner = Doctor.builder().id(1L).build();
        Schedule schedule = Schedule.builder().id(5L).doctor(owner).booked(false).build();
        when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));

        service.delete(5L, 1L);

        verify(scheduleRepository).delete(schedule);
    }

    @Test
    void deleteMissingSlotIsNotFound() {
        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
