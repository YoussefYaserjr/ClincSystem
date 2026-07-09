package com.clinicsystem.service;

import com.clinicsystem.dto.request.CreateMedicalRecordRequest;
import com.clinicsystem.dto.response.MedicalRecordResponse;
import java.util.List;

public interface MedicalRecordService {
    MedicalRecordResponse create(Long doctorId, CreateMedicalRecordRequest request);
    List<MedicalRecordResponse> getForPatient(Long requesterId, Long patientId);
    List<MedicalRecordResponse> getMineAsDoctor(Long doctorId);
    void delete(Long recordId, Long doctorId);
}