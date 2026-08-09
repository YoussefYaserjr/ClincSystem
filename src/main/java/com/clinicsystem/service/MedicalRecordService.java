package com.clinicsystem.service;

import com.clinicsystem.dto.request.CreateMedicalRecordRequest;
import com.clinicsystem.dto.response.MedicalRecordResponse;
import com.clinicsystem.dto.response.PageResponse;

public interface MedicalRecordService {
    MedicalRecordResponse create(Long doctorId, CreateMedicalRecordRequest request);
    PageResponse<MedicalRecordResponse> getForPatient(Long requesterId, Long patientId, int page, int size);
    PageResponse<MedicalRecordResponse> getMineAsDoctor(Long doctorId, int page, int size);
    void delete(Long recordId, Long doctorId);
}
