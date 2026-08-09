package com.clinicsystem.dto.response;

public record StatsResponse(
        long totalUsers,
        long totalPatients,
        long totalDoctors,
        long pendingDoctors,
        long totalAppointments,
        long pendingAppointments,
        long confirmedAppointments,
        long completedAppointments,
        long cancelledAppointments,
        long rejectedAppointments,
        long todayAppointments,
        long upcomingAppointments,
        long totalSchedules,
        long availableSlots,
        long totalMedicalRecords) {
}
