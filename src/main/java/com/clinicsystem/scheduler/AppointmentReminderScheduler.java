package com.clinicsystem.scheduler;

import com.clinicsystem.entity.Appointment;
import com.clinicsystem.entity.enums.AppointmentStatus;
import com.clinicsystem.repository.AppointmentRepository;
import com.clinicsystem.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AppointmentReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(AppointmentReminderScheduler.class);

    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;

    @Value("${app.notifications.reminder-hours-before:1}")
    int reminderHoursBefore;

    @Scheduled(fixedDelayString = "${app.notifications.reminder-interval-ms:60000}")
    public void sendUpcomingAppointmentReminders() {
        if (!notificationService.isEnabled()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusHours(reminderHoursBefore);

        List<Appointment> due = appointmentRepository.findDueReminders(
                AppointmentStatus.CONFIRMED,
                now.toLocalDate(), now.toLocalTime(),
                windowEnd.toLocalDate(), windowEnd.toLocalTime());

        for (Appointment appointment : due) {
            notificationService.appointmentReminder(appointment);
            appointment.setReminderSent(true);
            appointmentRepository.save(appointment);
            log.info("Reminder sent for appointment {}", appointment.getId());
        }
    }
}
