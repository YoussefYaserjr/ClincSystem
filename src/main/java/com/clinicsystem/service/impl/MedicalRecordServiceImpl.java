package com.clinicsystem.service.impl;

import com.clinicsystem.dto.request.CreateMedicalRecordRequest;
import com.clinicsystem.dto.response.MedicalRecordResponse;
import com.clinicsystem.entity.Appointment;
import com.clinicsystem.entity.Doctor;
import com.clinicsystem.entity.MedicalRecord;
import com.clinicsystem.entity.Patient;
import com.clinicsystem.exception.ResourceNotFoundException;
import com.clinicsystem.repository.AppointmentRepository;
import com.clinicsystem.repository.DoctorRepository;
import com.clinicsystem.repository.MedicalRecordRepository;
import com.clinicsystem.repository.PatientRepository;
import com.clinicsystem.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    @Transactional
    public MedicalRecordResponse create(Long doctorId, CreateMedicalRecordRequest request) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        Appointment appointment = null;
        if (request.getAppointmentId() != null) {
            appointment = appointmentRepository.findById(request.getAppointmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

            // A doctor can only attach a record to their own appointment with this patient.
            if (!appointment.getDoctor().getId().equals(doctorId)
                    || !appointment.getPatient().getId().equals(patient.getId())) {
                throw new AccessDeniedException("Appointment does not belong to this doctor/patient pair");
            }
        } else {
            throw new IllegalArgumentException("appointmentId is required to create a medical record");
        }

        MedicalRecord record = MedicalRecord.builder()
                .patient(patient)
                .doctor(doctor)
                .appointment(appointment)
                .diagnosis(request.getDiagnosis())
                .prescription(request.getPrescription())
                .notes(request.getNotes())
                .build();

        record = medicalRecordRepository.save(record);
        return toResponse(record);
    }

    @Override
    public List<MedicalRecordResponse> getForPatient(Long requesterId, Long patientId) {
        // A patient may only view their own records.
        // (Doctors viewing a specific patient's history would need a separate,
        // appointment-relationship-gated endpoint — not implemented here to
        // avoid quietly allowing any doctor to browse any patient's chart.)
        if (!requesterId.equals(patientId)) {
            throw new AccessDeniedException("You may only view your own medical records");
        }

        return medicalRecordRepository.findByPatientId(patientId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MedicalRecordResponse> getMineAsDoctor(Long doctorId) {
        return medicalRecordRepository.findByDoctorId(doctorId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long recordId, Long doctorId) {
        MedicalRecord record = medicalRecordRepository.findByIdAndDoctorId(recordId, doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Medical record not found or not owned by this doctor"));
        medicalRecordRepository.delete(record);
    }

    private MedicalRecordResponse toResponse(MedicalRecord r) {
        return MedicalRecordResponse.builder()
                .id(r.getId())
                .patientId(r.getPatient().getId())
                .patientName(r.getPatient().getName())
                .doctorId(r.getDoctor().getId())
                .doctorName(r.getDoctor().getName())
                .diagnosis(r.getDiagnosis())
                .prescription(r.getPrescription())
                .notes(r.getNotes())
                .createdAt(r.getCreatedAt())
                .build();
    }
}