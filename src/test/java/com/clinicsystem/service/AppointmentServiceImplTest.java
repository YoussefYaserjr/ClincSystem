package com.clinicsystem.service;

import com.clinicsystem.dto.request.BookAppointmentRequest;
import com.clinicsystem.dto.response.AppointmentResponse;
import com.clinicsystem.entity.Appointment;
import com.clinicsystem.entity.Doctor;
import com.clinicsystem.entity.Patient;
import com.clinicsystem.entity.Schedule;
import com.clinicsystem.entity.enums.AppointmentStatus;
import com.clinicsystem.exception.InvalidAppointmentStateException;
import com.clinicsystem.exception.ResourceNotFoundException;
import com.clinicsystem.exception.SlotAlreadyBookedException;
import com.clinicsystem.repository.AppointmentRepository;
import com.clinicsystem.repository.PatientRepository;
import com.clinicsystem.repository.ScheduleRepository;
import com.clinicsystem.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AppointmentServiceImplTest {

    private ScheduleRepository scheduleRepository;
    private AppointmentRepository appointmentRepository;
    private PatientRepository patientRepository;
    private NotificationService notificationService;
    private AppointmentServiceImpl service;

    @BeforeEach
    void setUp() {
        scheduleRepository = mock(ScheduleRepository.class);
        appointmentRepository = mock(AppointmentRepository.class);
        patientRepository = mock(PatientRepository.class);
        notificationService = mock(NotificationService.class);
        service = new AppointmentServiceImpl(
                scheduleRepository, appointmentRepository, patientRepository, notificationService);
    }

    @Test
    void bookMarksSlotBookedAndCreatesPendingAppointment() {
        Doctor doctor = Doctor.builder().id(1L).name("Dr. Smith").build();
        Schedule schedule = slot(10L, doctor, false);
        Patient patient = Patient.builder().id(2L).name("Patient One").build();

        when(scheduleRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(schedule));
        when(patientRepository.findById(2L)).thenReturn(Optional.of(patient));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> {
            Appointment saved = inv.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        BookAppointmentRequest request = new BookAppointmentRequest();
        request.setScheduleId(10L);
        request.setNotes("First visit");

        AppointmentResponse response = service.book(2L, request);

        assertThat(schedule.isBooked()).isTrue();
        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        verify(scheduleRepository).save(schedule);
        verify(notificationService).appointmentBooked(any(Appointment.class));
    }

    @Test
    void bookOnAlreadyBookedSlotThrowsConflict() {
        Schedule schedule = slot(10L, Doctor.builder().id(1L).build(), true);
        when(scheduleRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(schedule));

        BookAppointmentRequest request = new BookAppointmentRequest();
        request.setScheduleId(10L);

        assertThatThrownBy(() -> service.book(2L, request))
                .isInstanceOf(SlotAlreadyBookedException.class);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void bookOnMissingSlotThrowsNotFound() {
        when(scheduleRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        BookAppointmentRequest request = new BookAppointmentRequest();
        request.setScheduleId(999L);

        assertThatThrownBy(() -> service.book(2L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void confirmPendingAppointmentChangesStatus() {
        Appointment appt = appointment(AppointmentStatus.PENDING, 1L, 2L);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = service.confirm(5L, 1L);

        assertThat(response.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        verify(notificationService).appointmentConfirmed(appt);
    }

    @Test
    void confirmByNonOwnerDoctorIsForbidden() {
        Appointment appt = appointment(AppointmentStatus.PENDING, 1L, 2L);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appt));

        assertThatThrownBy(() -> service.confirm(5L, 99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cannotConfirmNonPendingAppointment() {
        Appointment appt = appointment(AppointmentStatus.COMPLETED, 1L, 2L);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appt));

        assertThatThrownBy(() -> service.confirm(5L, 1L))
                .isInstanceOf(InvalidAppointmentStateException.class);
    }

    @Test
    void cannotCompleteAPendingAppointment() {
        Appointment appt = appointment(AppointmentStatus.PENDING, 1L, 2L);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appt));

        assertThatThrownBy(() -> service.complete(5L, 1L))
                .isInstanceOf(InvalidAppointmentStateException.class);
    }

    @Test
    void cancelByPatientFreesSlotAndNotifiesDoctor() {
        Doctor doctor = Doctor.builder().id(1L).name("Dr. Smith").build();
        Schedule schedule = slot(10L, doctor, true);
        Appointment appt = Appointment.builder()
                .id(5L)
                .doctor(doctor)
                .patient(Patient.builder().id(2L).name("Patient One").build())
                .schedule(schedule)
                .status(AppointmentStatus.CONFIRMED)
                .build();
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appt));

        service.cancel(5L, 2L);

        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(schedule.isBooked()).isFalse();
        verify(scheduleRepository).save(schedule);
        verify(notificationService).appointmentCancelled(appt, true);
    }

    @Test
    void strangerCannotCancelAppointment() {
        Appointment appt = appointment(AppointmentStatus.PENDING, 1L, 2L);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appt));

        assertThatThrownBy(() -> service.cancel(5L, 55L))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Schedule slot(Long id, Doctor doctor, boolean booked) {
        return Schedule.builder()
                .id(id)
                .doctor(doctor)
                .availableDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(9, 30))
                .booked(booked)
                .build();
    }

    private Appointment appointment(AppointmentStatus status, Long doctorId, Long patientId) {
        return Appointment.builder()
                .id(5L)
                .doctor(Doctor.builder().id(doctorId).name("Dr. Smith").build())
                .patient(Patient.builder().id(patientId).name("Patient One").build())
                .schedule(slot(10L, Doctor.builder().id(doctorId).build(), status == AppointmentStatus.PENDING))
                .status(status)
                .build();
    }
}
