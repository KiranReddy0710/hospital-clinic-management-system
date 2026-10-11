CREATE DATABASE IF NOT EXISTS medicare_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE medicare_db;

CREATE TABLE IF NOT EXISTS patients (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  full_name VARCHAR(120) NOT NULL,
  age INT NOT NULL,
  gender VARCHAR(30) NOT NULL,
  phone VARCHAR(25) NOT NULL,
  email VARCHAR(160),
  blood_group VARCHAR(8),
  address VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_patients_name (full_name),
  INDEX idx_patients_phone (phone)
);

CREATE TABLE IF NOT EXISTS doctors (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  full_name VARCHAR(120) NOT NULL,
  specialization VARCHAR(100) NOT NULL,
  phone VARCHAR(25) NOT NULL,
  email VARCHAR(160),
  room_number VARCHAR(20),
  available BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_doctors_name (full_name),
  INDEX idx_doctors_specialization (specialization)
);

CREATE TABLE IF NOT EXISTS appointments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  appointment_at DATETIME NOT NULL,
  reason VARCHAR(255) NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'SCHEDULED',
  notes TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
  CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
  INDEX idx_appointments_time (appointment_at),
  INDEX idx_appointments_status (status)
);
