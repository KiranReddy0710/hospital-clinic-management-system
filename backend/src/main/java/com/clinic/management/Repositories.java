package com.clinic.management;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

interface PatientRepository extends JpaRepository<Patient, Long> {
    long countByCreatedAtIsNotNull();
}
interface DoctorRepository extends JpaRepository<Doctor, Long> {}
interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    long countByStatus(String status);
    List<Appointment> findTop8ByOrderByAppointmentAtAsc();
    long countByAppointmentAtBetween(LocalDateTime start, LocalDateTime end);
}
interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    java.util.Optional<UserAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
