package com.clinicsystem.service;

import com.clinicsystem.dto.response.DoctorResponse;
import com.clinicsystem.dto.response.PageResponse;
import com.clinicsystem.dto.response.StatsResponse;
import com.clinicsystem.dto.response.UserResponse;

public interface AdminService {

    PageResponse<UserResponse> listUsers(int page, int size);

    PageResponse<DoctorResponse> listDoctors(boolean approved, int page, int size);

    DoctorResponse approve(Long doctorId);

    DoctorResponse reject(Long doctorId);

    void deleteDoctor(Long doctorId);

    StatsResponse stats();
}
