package com.clinicsystem.service.impl;

import com.clinicsystem.dto.request.CreateMedicalRecordRequest;
import com.clinicsystem.dto.response.MedicalRecordResponse;
import com.clinicsystem.dto.response.PageResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

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
    public PageResponse<MedicalRecordResponse> getForPatient(Long requesterId, Long patientId, int page, int size) {
        // A patient may only view their own records.
        if (!requesterId.equals(patientId)) {
            throw new AccessDeniedException("You may only view your own medical records");
        }

        Page<MedicalRecord> records = medicalRecordRepository
                .findByPatientId(patientId, pageRequest(page, size));
        return PageResponse.of(records, this::toResponse);
    }

    @Override
    public PageResponse<MedicalRecordResponse> getMineAsDoctor(Long doctorId, int page, int size) {
        Page<MedicalRecord> records = medicalRecordRepository
                .findByDoctorId(doctorId, pageRequest(page, size));
        return PageResponse.of(records, this::toResponse);
    }

    @Override
    @Transactional
    public void delete(Long recordId, Long doctorId) {
        MedicalRecord record = medicalRecordRepository.findByIdAndDoctorId(recordId, doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Medical record not found or not owned by this doctor"));
        medicalRecordRepository.delete(record);
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(page, size, NEWEST_FIRST);
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
