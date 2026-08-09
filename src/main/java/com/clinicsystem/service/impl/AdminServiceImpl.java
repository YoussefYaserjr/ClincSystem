package com.clinicsystem.service.impl;

import com.clinicsystem.dto.response.DoctorResponse;
import com.clinicsystem.dto.response.PageResponse;
import com.clinicsystem.dto.response.StatsResponse;
import com.clinicsystem.dto.response.UserResponse;
import com.clinicsystem.entity.Doctor;
import com.clinicsystem.entity.User;
import com.clinicsystem.entity.enums.AppointmentStatus;
import com.clinicsystem.entity.enums.Role;
import com.clinicsystem.exception.ResourceInUseException;
import com.clinicsystem.exception.ResourceNotFoundException;
import com.clinicsystem.repository.AppointmentRepository;
import com.clinicsystem.repository.DoctorRepository;
import com.clinicsystem.repository.MedicalRecordRepository;
import com.clinicsystem.repository.ScheduleRepository;
import com.clinicsystem.repository.UserRepository;
import com.clinicsystem.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final ScheduleRepository scheduleRepository;
    private final MedicalRecordRepository medicalRecordRepository;

    @Override
    public PageResponse<UserResponse> listUsers(int page, int size) {
        Page<User> users = userRepository.findAll(PageRequest.of(page, size, Sort.by("id")));
        return PageResponse.of(users, UserResponse::from);
    }

    @Override
    public PageResponse<DoctorResponse> listDoctors(boolean approved, int page, int size) {
        Page<Doctor> doctors = doctorRepository.findByApproved(approved, PageRequest.of(page, size, Sort.by("name")));
        return PageResponse.of(doctors, this::toDoctorResponse);
    }

    @Override
    @Transactional
    public DoctorResponse approve(Long doctorId) {
        Doctor doctor = requireDoctor(doctorId);
        doctor.setApproved(true);
        return toDoctorResponse(doctorRepository.save(doctor));
    }

    @Override
    @Transactional
    public DoctorResponse reject(Long doctorId) {
        Doctor doctor = requireDoctor(doctorId);
        doctor.setApproved(false);
        return toDoctorResponse(doctorRepository.save(doctor));
    }

    @Override
    @Transactional
    public void deleteDoctor(Long doctorId) {
        Doctor doctor = requireDoctor(doctorId);

        if (appointmentRepository.existsByDoctorId(doctorId)
                || scheduleRepository.existsByDoctorId(doctorId)
                || medicalRecordRepository.existsByDoctorId(doctorId)) {
            throw new ResourceInUseException(
                    "Doctor has appointments, schedules or medical records and cannot be deleted");
        }

        doctorRepository.delete(doctor);
    }

    @Override
    public StatsResponse stats() {
        LocalDate today = LocalDate.now();
        return new StatsResponse(
                userRepository.count(),
                userRepository.countByRole(Role.PATIENT),
                userRepository.countByRole(Role.DOCTOR),
                doctorRepository.countByApprovedFalse(),
                appointmentRepository.count(),
                appointmentRepository.countByStatus(AppointmentStatus.PENDING),
                appointmentRepository.countByStatus(AppointmentStatus.CONFIRMED),
                appointmentRepository.countByStatus(AppointmentStatus.COMPLETED),
                appointmentRepository.countByStatus(AppointmentStatus.CANCELLED),
                appointmentRepository.countByStatus(AppointmentStatus.REJECTED),
                appointmentRepository.countBySchedule_AvailableDate(today),
                appointmentRepository.countBySchedule_AvailableDateGreaterThanEqual(today),
                scheduleRepository.count(),
                scheduleRepository.countByBookedFalse(),
                medicalRecordRepository.count());
    }

    private Doctor requireDoctor(Long doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));
    }

    private DoctorResponse toDoctorResponse(Doctor d) {
        return DoctorResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .email(d.getEmail())
                .phone(d.getPhone())
                .specialty(d.getSpecialty())
                .location(d.getLocation())
                .clinic(d.getClinic())
                .experience(d.getExperience())
                .consultationFee(d.getConsultationFee())
                .rating(d.getRating())
                .approved(d.isApproved())
                .build();
    }
}
