package com.clinic.management;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "patients")
class Patient {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @NotBlank @Size(max=120) @Column(name="full_name", nullable=false)
    public String fullName;
    @NotNull @Min(0) @Max(120)
    public Integer age;
    @NotBlank public String gender;
    @NotBlank @Size(max=25) public String phone;
    @Email public String email;
    @Size(max=8) @Column(name="blood_group") public String bloodGroup;
    @Size(max=255) public String address;
    @Column(name="created_at", insertable=false, updatable=false) public LocalDateTime createdAt;
    public Patient() {}
}

@Entity
@Table(name = "doctors")
class Doctor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @NotBlank @Size(max=120) @Column(name="full_name", nullable=false)
    public String fullName;
    @NotBlank @Size(max=100) @Column(nullable=false)
    public String specialization;
    @NotBlank @Size(max=25) public String phone;
    @Email public String email;
    @Column(name="room_number") public String roomNumber;
    @Column(nullable=false) public boolean available = true;
    @Column(name="created_at", insertable=false, updatable=false) public LocalDateTime createdAt;
    public Doctor() {}
}

@Entity
@Table(name = "appointments")
class Appointment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @NotNull @Column(name="patient_id", nullable=false) public Long patientId;
    @NotNull @Column(name="doctor_id", nullable=false) public Long doctorId;
    @NotNull @Column(name="appointment_at", nullable=false) public LocalDateTime appointmentAt;
    @NotBlank @Size(max=255) public String reason;
    @NotBlank public String status = "SCHEDULED";
    @Column(columnDefinition="TEXT") public String notes;
    @Column(name="created_at", insertable=false, updatable=false) public LocalDateTime createdAt;
    public Appointment() {}
}
