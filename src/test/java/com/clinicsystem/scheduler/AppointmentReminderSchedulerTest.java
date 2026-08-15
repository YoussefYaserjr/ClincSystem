package com.clinicsystem.scheduler;

import com.clinicsystem.entity.Appointment;
import com.clinicsystem.entity.Doctor;
import com.clinicsystem.entity.Patient;
import com.clinicsystem.entity.Schedule;
import com.clinicsystem.entity.enums.AppointmentStatus;
import com.clinicsystem.repository.AppointmentRepository;
import com.clinicsystem.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AppointmentReminderSchedulerTest {

    private AppointmentRepository appointmentRepository;
    private NotificationService notificationService;
    private AppointmentReminderScheduler scheduler;

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        notificationService = mock(NotificationService.class);
        scheduler = new AppointmentReminderScheduler(appointmentRepository, notificationService);
        scheduler.reminderHoursBefore = 1;
    }

    @Test
    void sendsReminderAndMarksAppointmentWhenDue() {
        when(notificationService.isEnabled()).thenReturn(true);
        Appointment due = appointmentWithTime(LocalDate.now().plusDays(1), LocalTime.of(10, 0));
        when(appointmentRepository.findDueReminders(eq(AppointmentStatus.CONFIRMED), any(), any(), any(), any()))
                .thenReturn(List.of(due));

        scheduler.sendUpcomingAppointmentReminders();

        verify(notificationService).appointmentReminder(due);
        assertThat(due.isReminderSent()).isTrue();
        verify(appointmentRepository).save(due);
    }

    @Test
    void doesNothingWhenNotificationsDisabled() {
        when(notificationService.isEnabled()).thenReturn(false);

        scheduler.sendUpcomingAppointmentReminders();

        verify(appointmentRepository, never()).findDueReminders(any(), any(), any(), any(), any());
        verify(notificationService, never()).appointmentReminder(any(Appointment.class));
    }

    @Test
    void doesNotMarkAppointmentsThatAreNotDue() {
        when(notificationService.isEnabled()).thenReturn(true);
        when(appointmentRepository.findDueReminders(eq(AppointmentStatus.CONFIRMED), any(), any(), any(), any()))
                .thenReturn(List.of());

        scheduler.sendUpcomingAppointmentReminders();

        verify(notificationService, never()).appointmentReminder(any(Appointment.class));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    private Appointment appointmentWithTime(LocalDate date, LocalTime time) {
        return Appointment.builder()
                .id(1L)
                .doctor(Doctor.builder().id(1L).name("Dr. Smith").email("doc@test.com").build())
                .patient(Patient.builder().id(2L).name("Patient One").email("patient@test.com").build())
                .schedule(Schedule.builder()
                        .id(10L)
                        .availableDate(date)
                        .startTime(time)
                        .build())
                .status(AppointmentStatus.CONFIRMED)
                .reminderSent(false)
                .build();
    }
}
