package com.clinicsystem.service;

import com.clinicsystem.entity.Appointment;

public interface NotificationService {
    void appointmentBooked(Appointment appointment);
    void appointmentConfirmed(Appointment appointment);
    void appointmentRejected(Appointment appointment);
    void appointmentCompleted(Appointment appointment);
    void appointmentCancelled(Appointment appointment, boolean cancelledByPatient);
}
