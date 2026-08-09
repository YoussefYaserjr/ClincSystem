package com.clinicsystem.service.impl;

import com.clinicsystem.entity.Appointment;
import com.clinicsystem.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.notifications.enabled:false}")
    private boolean enabled;

    @Value("${app.notifications.from:no-reply@clinicsystem.local}")
    private String from;

    @Override
    public void appointmentBooked(Appointment a) {
        send(a.getDoctor().getEmail(), "New appointment request",
                String.format("A patient (%s) requested an appointment at %s. Confirm or reject it in the system.",
                        a.getPatient().getName(), time(a)));
    }

    @Override
    public void appointmentConfirmed(Appointment a) {
        send(a.getPatient().getEmail(), "Appointment confirmed",
                String.format("Your appointment with Dr. %s at %s has been confirmed.",
                        a.getDoctor().getName(), time(a)));
    }

    @Override
    public void appointmentRejected(Appointment a) {
        send(a.getPatient().getEmail(), "Appointment rejected",
                String.format("Your appointment with Dr. %s at %s was rejected.",
                        a.getDoctor().getName(), time(a)));
    }

    @Override
    public void appointmentCompleted(Appointment a) {
        send(a.getPatient().getEmail(), "Appointment completed",
                String.format("Your appointment with Dr. %s at %s has been completed.",
                        a.getDoctor().getName(), time(a)));
    }

    @Override
    public void appointmentCancelled(Appointment a, boolean cancelledByPatient) {
        String to = cancelledByPatient ? a.getDoctor().getEmail() : a.getPatient().getEmail();
        String who = cancelledByPatient ? "patient" : "doctor";
        send(to, "Appointment cancelled",
                String.format("The appointment at %s was cancelled by the %s.",
                        time(a), who));
    }

    private String time(Appointment a) {
        return a.getSchedule().getAvailableDate() + " " + a.getSchedule().getStartTime();
    }

    private void send(String to, String subject, String body) {
        if (!enabled) {
            log.info("Email notifications disabled - would send '{}' to {}", subject, to);
            return;
        }

        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.warn("app.notifications.enabled=true but no JavaMailSender bean is configured; cannot send '{}'", subject);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email '{}' to {}: {}", subject, to, e.getMessage());
        }
    }
}
