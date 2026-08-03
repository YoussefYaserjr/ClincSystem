package com.clinicsystem.service.impl;

import com.clinicsystem.dto.request.BookAppointmentRequest;
import com.clinicsystem.dto.response.AppointmentResponse;
import com.clinicsystem.entity.Appointment;
import com.clinicsystem.entity.Patient;
import com.clinicsystem.entity.Schedule;
import com.clinicsystem.entity.enums.AppointmentStatus;
import com.clinicsystem.exception.InvalidAppointmentStateException;
import com.clinicsystem.exception.ResourceNotFoundException;
import com.clinicsystem.exception.SlotAlreadyBookedException;
import com.clinicsystem.repository.AppointmentRepository;
import com.clinicsystem.repository.PatientRepository;
import com.clinicsystem.repository.ScheduleRepository;
import com.clinicsystem.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private static final EnumSet<AppointmentStatus> CANCELLABLE =
            EnumSet.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

    private final ScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;

    @Override
    @Transactional
    public AppointmentResponse book(Long patientId, BookAppointmentRequest request) {

        // Lock the slot row — any concurrent booking attempt on the same
        // scheduleId blocks here until this transaction commits/rolls back.
        Schedule schedule = scheduleRepository.findByIdForUpdate(request.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule slot not found"));

        if (schedule.isBooked()) {
            throw new SlotAlreadyBookedException("This slot was just booked by someone else");
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        schedule.setBooked(true);
        scheduleRepository.save(schedule);

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(schedule.getDoctor())
                .schedule(schedule)
                .status(AppointmentStatus.PENDING)
                .notes(request.getNotes())
                .createdAt(LocalDateTime.now())
                .build();

        // Unique constraint on schedule_id is the DB-level backstop
        // if two requests somehow raced past the lock above.
        appointment = appointmentRepository.save(appointment);

        return toResponse(appointment);
    }

    @Override
    @Transactional
    public void cancel(Long appointmentId, Long requesterId) {
        Appointment appt = requireAppointment(appointmentId);

        boolean isPatientOwner = appt.getPatient().getId().equals(requesterId);
        boolean isDoctorOwner = appt.getDoctor().getId().equals(requesterId);

        if (!isPatientOwner && !isDoctorOwner) {
            throw new AccessDeniedException("You are not part of this appointment");
        }

        requireStatus(appt, CANCELLABLE, "Only PENDING or CONFIRMED appointments can be cancelled");

        appt.setStatus(AppointmentStatus.CANCELLED);
        freeSchedule(appt);
        appointmentRepository.save(appt);
    }

    @Override
    @Transactional
    public AppointmentResponse confirm(Long appointmentId, Long doctorId) {
        Appointment appt = requireOwnedByDoctor(appointmentId, doctorId);
        requireStatus(appt, EnumSet.of(AppointmentStatus.PENDING),
                "Only PENDING appointments can be confirmed");

        appt.setStatus(AppointmentStatus.CONFIRMED);
        return toResponse(appointmentRepository.save(appt));
    }

    @Override
    @Transactional
    public AppointmentResponse reject(Long appointmentId, Long doctorId) {
        Appointment appt = requireOwnedByDoctor(appointmentId, doctorId);
        requireStatus(appt, EnumSet.of(AppointmentStatus.PENDING),
                "Only PENDING appointments can be rejected");

        appt.setStatus(AppointmentStatus.REJECTED);
        freeSchedule(appt);
        return toResponse(appointmentRepository.save(appt));
    }

    @Override
    @Transactional
    public AppointmentResponse complete(Long appointmentId, Long doctorId) {
        Appointment appt = requireOwnedByDoctor(appointmentId, doctorId);
        requireStatus(appt, EnumSet.of(AppointmentStatus.CONFIRMED),
                "Only CONFIRMED appointments can be completed");

        appt.setStatus(AppointmentStatus.COMPLETED);
        // Slot stays booked — the visit happened; free it only on cancel/reject.
        return toResponse(appointmentRepository.save(appt));
    }

    @Override
    public List<AppointmentResponse> getForPatient(Long patientId) {
        return appointmentRepository.findByPatientId(patientId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<AppointmentResponse> getForDoctor(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private Appointment requireAppointment(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
    }

    private Appointment requireOwnedByDoctor(Long appointmentId, Long doctorId) {
        Appointment appt = requireAppointment(appointmentId);
        if (!appt.getDoctor().getId().equals(doctorId)) {
            throw new AccessDeniedException("You are not the doctor for this appointment");
        }
        return appt;
    }

    private void requireStatus(Appointment appt, EnumSet<AppointmentStatus> allowed, String message) {
        if (!allowed.contains(appt.getStatus())) {
            throw new InvalidAppointmentStateException(
                    message + " (current status: " + appt.getStatus() + ")");
        }
    }

    private void freeSchedule(Appointment appt) {
        Schedule schedule = appt.getSchedule();
        schedule.setBooked(false);
        scheduleRepository.save(schedule);
    }

    private AppointmentResponse toResponse(Appointment a) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .doctorId(a.getDoctor().getId())
                .doctorName(a.getDoctor().getName())
                .patientId(a.getPatient().getId())
                .patientName(a.getPatient().getName())
                .appointmentTime(LocalDateTime.of(
                        a.getSchedule().getAvailableDate(), a.getSchedule().getStartTime()))
                .status(a.getStatus())
                .build();
    }
}