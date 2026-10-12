package com.clinic.management;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api")
class ApiController {
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final UserAccountRepository users;

    ApiController(PatientRepository patients, DoctorRepository doctors, AppointmentRepository appointments, UserAccountRepository users) {
        this.patients = patients;
        this.doctors = doctors;
        this.appointments = appointments;
        this.users = users;
    }

    @GetMapping("/health")
    Map<String,Object> health() { return Map.of("status","UP","application","MediCare Management API"); }

    @GetMapping("/dashboard")
    Map<String,Object> dashboard(Authentication auth) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        if(auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_DOCTOR"))) {
            UserAccount user=users.findByEmailIgnoreCase(auth.getName()).orElseThrow(()->notFound("Account"));
            List<Appointment> own=appointments.findAll().stream().filter(a->Objects.equals(a.doctorId,user.profileId)).toList();
            return Map.of("patients",own.stream().map(a->a.patientId).distinct().count(),"doctors",1,
                "appointmentsToday",own.stream().filter(a->!a.appointmentAt.isBefore(start)&&a.appointmentAt.isBefore(end)).count(),
                "scheduledAppointments",own.stream().filter(a->"SCHEDULED".equals(a.status)).count(),
                "completedAppointments",own.stream().filter(a->"COMPLETED".equals(a.status)).count(),
                "cancelledAppointments",own.stream().filter(a->"CANCELLED".equals(a.status)).count());
        }
        return Map.of(
            "patients", patients.count(),
            "doctors", doctors.count(),
            "appointmentsToday", appointments.countByAppointmentAtBetween(start,end),
            "scheduledAppointments", appointments.countByStatus("SCHEDULED"),
            "completedAppointments", appointments.countByStatus("COMPLETED"),
            "cancelledAppointments", appointments.countByStatus("CANCELLED")
        );
    }

    @GetMapping("/patients") List<Patient> allPatients(Authentication auth) {
        if(auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"))) return patients.findAll();
        UserAccount user=users.findByEmailIgnoreCase(auth.getName()).orElseThrow(()->notFound("Account"));
        Set<Long> ids=appointments.findAll().stream().filter(a->Objects.equals(a.doctorId,user.profileId)).map(a->a.patientId).collect(java.util.stream.Collectors.toSet());
        return patients.findAllById(ids);
    }
    @GetMapping("/patients/me") Patient myPatient(Authentication auth) {
        UserAccount user=users.findByEmailIgnoreCase(auth.getName()).orElseThrow(()->notFound("Account"));
        return patients.findById(user.profileId).orElseThrow(()->notFound("Patient"));
    }
    @GetMapping("/patients/{id}") Patient patient(@PathVariable Long id, Authentication auth) {
        if(auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_DOCTOR"))) {
            UserAccount user=users.findByEmailIgnoreCase(auth.getName()).orElseThrow(()->notFound("Account"));
            if(appointments.findAll().stream().noneMatch(a->Objects.equals(a.patientId,id)&&Objects.equals(a.doctorId,user.profileId))) throw notFound("Patient");
        }
        return patients.findById(id).orElseThrow(() -> notFound("Patient"));
    }
    @PostMapping("/patients") @ResponseStatus(HttpStatus.CREATED)
    Patient createPatient(@Valid @RequestBody Patient p) { p.id=null; return patients.save(p); }
    @PutMapping("/patients/{id}") Patient updatePatient(@PathVariable Long id, @Valid @RequestBody Patient p) {
        if (!patients.existsById(id)) throw notFound("Patient"); p.id=id; return patients.save(p);
    }
    @DeleteMapping("/patients/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void deletePatient(@PathVariable Long id) {
        if (!patients.existsById(id)) throw notFound("Patient");
        if (appointments.findAll().stream().anyMatch(a -> Objects.equals(a.patientId,id)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Patient has appointments. Cancel or remove appointments first.");
        patients.deleteById(id);
    }

    @GetMapping("/doctors") List<Doctor> allDoctors() { return doctors.findAll(); }
    @GetMapping("/doctors/{id}") Doctor doctor(@PathVariable Long id) { return doctors.findById(id).orElseThrow(() -> notFound("Doctor")); }
    @PostMapping("/doctors") @ResponseStatus(HttpStatus.CREATED)
    Doctor createDoctor(@Valid @RequestBody Doctor d) { d.id=null; return doctors.save(d); }
    @PutMapping("/doctors/{id}") Doctor updateDoctor(@PathVariable Long id, @Valid @RequestBody Doctor d) {
        if (!doctors.existsById(id)) throw notFound("Doctor"); d.id=id; return doctors.save(d);
    }
    @DeleteMapping("/doctors/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteDoctor(@PathVariable Long id) {
        if (!doctors.existsById(id)) throw notFound("Doctor");
        if (appointments.findAll().stream().anyMatch(a -> Objects.equals(a.doctorId,id)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Doctor has appointments. Remove appointments first.");
        doctors.deleteById(id);
    }

    @GetMapping("/appointments") List<Appointment> allAppointments(Authentication auth) {
        if(auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"))) return appointments.findAll();
        UserAccount user=users.findByEmailIgnoreCase(auth.getName()).orElseThrow(()->notFound("Account"));
        return appointments.findAll().stream().filter(a->Objects.equals(a.doctorId,user.profileId)).toList();
    }
    @GetMapping("/appointments/mine") List<Appointment> myAppointments(Authentication auth) {
        UserAccount user=users.findByEmailIgnoreCase(auth.getName()).orElseThrow(()->notFound("Account"));
        if(user.role==AccountRole.DOCTOR) return appointments.findAll().stream().filter(a->Objects.equals(a.doctorId,user.profileId)).toList();
        return appointments.findAll().stream().filter(a->Objects.equals(a.patientId,user.profileId)).toList();
    }
    @GetMapping("/appointments/{id}") Appointment appointment(@PathVariable Long id, Authentication auth) {
        Appointment item=appointments.findById(id).orElseThrow(()->notFound("Appointment"));
        if(auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_DOCTOR"))) {
            UserAccount user=users.findByEmailIgnoreCase(auth.getName()).orElseThrow(()->notFound("Account"));
            if(!Objects.equals(item.doctorId,user.profileId)) throw notFound("Appointment");
        }
        return item;
    }
    @PostMapping("/appointments") @ResponseStatus(HttpStatus.CREATED)
    Appointment createAppointment(@Valid @RequestBody Appointment a, Authentication auth) {
        if(auth.getAuthorities().stream().anyMatch(authority->authority.getAuthority().equals("ROLE_PATIENT"))) {
            UserAccount user=users.findByEmailIgnoreCase(auth.getName()).orElseThrow(()->notFound("Account"));
            a.patientId=user.profileId;
            a.status="SCHEDULED";
        }
        if (!patients.existsById(a.patientId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Patient does not exist");
        if (!doctors.existsById(a.doctorId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Doctor does not exist");
        if (!Set.of("SCHEDULED","COMPLETED","CANCELLED").contains(a.status)) a.status="SCHEDULED";
        a.id=null; return appointments.save(a);
    }
    @PutMapping("/appointments/{id}") Appointment updateAppointment(@PathVariable Long id, @Valid @RequestBody Appointment a, Authentication auth) {
        if (!appointments.existsById(id)) throw notFound("Appointment");
        if(auth.getAuthorities().stream().anyMatch(authority->authority.getAuthority().equals("ROLE_DOCTOR"))) {
            UserAccount user=users.findByEmailIgnoreCase(auth.getName()).orElseThrow(()->notFound("Account"));
            Appointment existing=appointments.findById(id).orElseThrow(()->notFound("Appointment"));
            if(!Objects.equals(existing.doctorId,user.profileId)) throw notFound("Appointment");
            a.patientId=existing.patientId; a.doctorId=existing.doctorId;
        }
        if (!patients.existsById(a.patientId) || !doctors.existsById(a.doctorId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Patient or doctor does not exist");
        if (!Set.of("SCHEDULED","COMPLETED","CANCELLED").contains(a.status))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid status");
        a.id=id; return appointments.save(a);
    }
    @DeleteMapping("/appointments/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAppointment(@PathVariable Long id) {
        if (!appointments.existsById(id)) throw notFound("Appointment"); appointments.deleteById(id);
    }

    private ResponseStatusException notFound(String entity) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, entity + " not found");
    }
}
