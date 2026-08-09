package com.clinicsystem.service;

import com.clinicsystem.dto.response.DoctorResponse;
import com.clinicsystem.dto.response.PageResponse;
import com.clinicsystem.dto.response.StatsResponse;
import com.clinicsystem.entity.Doctor;
import com.clinicsystem.entity.enums.Role;
import com.clinicsystem.exception.ResourceInUseException;
import com.clinicsystem.exception.ResourceNotFoundException;
import com.clinicsystem.repository.AppointmentRepository;
import com.clinicsystem.repository.DoctorRepository;
import com.clinicsystem.repository.MedicalRecordRepository;
import com.clinicsystem.repository.ScheduleRepository;
import com.clinicsystem.repository.UserRepository;
import com.clinicsystem.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AdminServiceImplTest {

    private UserRepository userRepository;
    private DoctorRepository doctorRepository;
    private AppointmentRepository appointmentRepository;
    private ScheduleRepository scheduleRepository;
    private MedicalRecordRepository medicalRecordRepository;
    private AdminServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        doctorRepository = mock(DoctorRepository.class);
        appointmentRepository = mock(AppointmentRepository.class);
        scheduleRepository = mock(ScheduleRepository.class);
        medicalRecordRepository = mock(MedicalRecordRepository.class);
        service = new AdminServiceImpl(userRepository, doctorRepository,
                appointmentRepository, scheduleRepository, medicalRecordRepository);
    }

    @Test
    void approveSetsDoctorApproved() {
        Doctor doctor = Doctor.builder().id(1L).name("Dr. Smith").approved(false).build();
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(inv -> inv.getArgument(0));

        DoctorResponse response = service.approve(1L);

        assertThat(response.isApproved()).isTrue();
        verify(doctorRepository).save(doctor);
    }

    @Test
    void rejectWithdrawsApproval() {
        Doctor doctor = Doctor.builder().id(1L).name("Dr. Smith").approved(true).build();
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(inv -> inv.getArgument(0));

        DoctorResponse response = service.reject(1L);

        assertThat(response.isApproved()).isFalse();
    }

    @Test
    void approveMissingDoctorIsNotFound() {
        when(doctorRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteDoctorInUseIsRejected() {
        Doctor doctor = Doctor.builder().id(1L).build();
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsByDoctorId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteDoctor(1L))
                .isInstanceOf(ResourceInUseException.class);
        verify(doctorRepository, never()).delete(any());
    }

    @Test
    void deleteDoctorWithoutReferencesSucceeds() {
        Doctor doctor = Doctor.builder().id(1L).build();
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsByDoctorId(1L)).thenReturn(false);
        when(scheduleRepository.existsByDoctorId(1L)).thenReturn(false);
        when(medicalRecordRepository.existsByDoctorId(1L)).thenReturn(false);

        service.deleteDoctor(1L);

        verify(doctorRepository).delete(doctor);
    }

    @Test
    void statsAggregatesCounts() {
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByRole(Role.PATIENT)).thenReturn(70L);
        when(userRepository.countByRole(Role.DOCTOR)).thenReturn(30L);
        when(doctorRepository.countByApprovedFalse()).thenReturn(5L);
        when(appointmentRepository.count()).thenReturn(50L);
        when(scheduleRepository.count()).thenReturn(200L);
        when(scheduleRepository.countByBookedFalse()).thenReturn(120L);
        when(medicalRecordRepository.count()).thenReturn(40L);

        StatsResponse stats = service.stats();

        assertThat(stats.totalUsers()).isEqualTo(100L);
        assertThat(stats.totalPatients()).isEqualTo(70L);
        assertThat(stats.totalDoctors()).isEqualTo(30L);
        assertThat(stats.pendingDoctors()).isEqualTo(5L);
        assertThat(stats.totalAppointments()).isEqualTo(50L);
        assertThat(stats.totalSchedules()).isEqualTo(200L);
        assertThat(stats.availableSlots()).isEqualTo(120L);
        assertThat(stats.totalMedicalRecords()).isEqualTo(40L);
    }

    @Test
    void listDoctorsPassesApprovalFlagToRepository() {
        when(doctorRepository.findByApproved(anyBoolean(), any())).thenReturn(Page.empty());

        PageResponse<DoctorResponse> response = service.listDoctors(false, 0, 20);

        assertThat(response.content()).isEmpty();
        verify(doctorRepository).findByApproved(eq(false), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void listUsersReturnsPagedUsers() {
        when(userRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(Page.empty());

        PageResponse<com.clinicsystem.dto.response.UserResponse> response = service.listUsers(0, 20);

        assertThat(response.content()).isEmpty();
        verify(userRepository).findAll(any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void listUsersMapsRoles() {
        Doctor doctor = Doctor.builder().id(1L).name("Dr. Smith").email("d@test.com").role(Role.DOCTOR).build();
        when(userRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(doctor)));

        PageResponse<com.clinicsystem.dto.response.UserResponse> response = service.listUsers(0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).getRole()).isEqualTo(Role.DOCTOR);
    }
}
