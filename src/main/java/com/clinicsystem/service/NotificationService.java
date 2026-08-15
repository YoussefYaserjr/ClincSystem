package com.clinicsystem.service;

import com.clinicsystem.entity.Appointment;
import com.clinicsystem.entity.User;

public interface NotificationService {
    void userLoggedIn(User user);
    void appointmentBooked(Appointment appointment);
    void appointmentConfirmed(Appointment appointment);
    void appointmentRejected(Appointment appointment);
    void appointmentCompleted(Appointment appointment);
    void appointmentCancelled(Appointment appointment, boolean cancelledByPatient);
    void appointmentReminder(Appointment appointment);
    boolean isEnabled();
}
