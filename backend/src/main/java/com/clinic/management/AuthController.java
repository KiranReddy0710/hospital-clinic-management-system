package com.clinic.management;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Email;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
class AuthController {
    private final UserAccountRepository users;
    private final RegistrationInviteRepository invites;
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final PasswordEncoder passwords;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contexts = new HttpSessionSecurityContextRepository();
    private final ChangeSessionIdAuthenticationStrategy sessionStrategy = new ChangeSessionIdAuthenticationStrategy();

    AuthController(UserAccountRepository users, RegistrationInviteRepository invites,
                   PatientRepository patients, DoctorRepository doctors, PasswordEncoder passwords,
                   AuthenticationManager authenticationManager) {
        this.users=users; this.invites=invites; this.patients=patients; this.doctors=doctors;
        this.passwords=passwords; this.authenticationManager=authenticationManager;
    }

    @GetMapping("/me") SafeAccount me(Authentication authentication) {
        return SafeAccount.from(account(authentication.getName()));
    }

    @PostMapping("/register/patient") @ResponseStatus(HttpStatus.CREATED) @Transactional
    SafeAccount registerPatient(@RequestBody PatientRegistration input, HttpServletRequest request, HttpServletResponse response) {
        validateCommon(input.email(),input.password(),input.fullName());
        if(input.age()==null || input.age()<0 || input.age()>120 || blank(input.gender()) || blank(input.phone()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Complete the required patient details.");
        ensureEmailAvailable(input.email());
        Patient patient=new Patient(); patient.fullName=input.fullName().trim(); patient.age=input.age();
        patient.gender=input.gender().trim(); patient.phone=input.phone().trim(); patient.email=normalize(input.email());
        patient.bloodGroup=input.bloodGroup(); patient.address=input.address(); patient=patients.save(patient);
        UserAccount user=newAccount(input.email(),input.password(),input.fullName(),AccountRole.PATIENT,patient.id);
        users.save(user); authenticate(user,request,response); return SafeAccount.from(user);
    }

    @PostMapping("/register/open") @ResponseStatus(HttpStatus.CREATED) @Transactional
    SafeAccount registerOpen(@RequestBody Map<String,String> input, HttpServletRequest request, HttpServletResponse response) {
        AccountRole role;
        try { role=AccountRole.valueOf(input.getOrDefault("role", "PATIENT").trim().toUpperCase()); }
        catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose Patient or Doctor."); }
        if(role!=AccountRole.PATIENT && role!=AccountRole.DOCTOR)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose Patient or Doctor.");
        validateCommon(input.get("email"),input.get("password"),input.get("fullName")); ensureEmailAvailable(input.get("email"));
        Long profileId=null;
        if(role==AccountRole.PATIENT) {
            int age;
            try { age=Integer.parseInt(input.getOrDefault("age", "-1")); } catch (NumberFormatException ex) { age=-1; }
            if(age<0 || age>120 || blank(input.get("gender")) || blank(input.get("phone")))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Complete the required patient details.");
            Patient patient=new Patient(); patient.fullName=input.get("fullName").trim(); patient.age=age;
            patient.gender=input.get("gender").trim(); patient.phone=input.get("phone").trim(); patient.email=normalize(input.get("email"));
            patient=patients.save(patient); profileId=patient.id;
        } else if(role==AccountRole.DOCTOR) {
            if(blank(input.get("phone")) || blank(input.get("specialization")))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Phone and specialization are required.");
            Doctor doctor=new Doctor(); doctor.fullName=input.get("fullName").trim(); doctor.email=normalize(input.get("email"));
            doctor.phone=input.get("phone").trim(); doctor.specialization=input.get("specialization").trim();
            doctor=doctors.save(doctor); profileId=doctor.id;
        }
        UserAccount user=newAccount(input.get("email"),input.get("password"),input.get("fullName"),role,profileId);
        users.save(user); authenticate(user,request,response); return SafeAccount.from(user);
    }

    @PostMapping("/register/invited") @ResponseStatus(HttpStatus.CREATED) @Transactional
    SafeAccount registerInvited(@RequestBody InvitedRegistration input, HttpServletRequest request, HttpServletResponse response) {
        validateCommon(input.email(),input.password(),input.fullName()); ensureEmailAvailable(input.email());
        RegistrationInvite invite=invites.findByTokenHash(hash(input.inviteCode()==null?"":input.inviteCode()))
            .filter(i->i.role==AccountRole.DOCTOR && i.usedAt==null && i.expiresAt.isAfter(Instant.now()))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"Invite code is invalid, expired, or already used."));
        Long profileId=null;
        if(invite.role==AccountRole.DOCTOR) {
            if(blank(input.phone()) || blank(input.specialization())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Phone and specialization are required.");
            Doctor doctor=new Doctor(); doctor.fullName=input.fullName().trim(); doctor.email=normalize(input.email());
            doctor.phone=input.phone().trim(); doctor.specialization=input.specialization().trim(); doctor=doctors.save(doctor); profileId=doctor.id;
        }
        UserAccount user=newAccount(input.email(),input.password(),input.fullName(),invite.role,profileId);
        users.save(user); invite.usedAt=Instant.now(); invites.save(invite); authenticate(user,request,response); return SafeAccount.from(user);
    }

    @PostMapping("/login") SafeAccount login(@RequestBody LoginRequest input, HttpServletRequest request, HttpServletResponse response) {
        if(input.email()==null || input.password()==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter your Gmail and password.");
        Authentication auth=authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(normalize(input.email()),input.password()));
        sessionStrategy.onAuthentication(auth,request,response);
        SecurityContext context=SecurityContextHolder.createEmptyContext(); context.setAuthentication(auth); SecurityContextHolder.setContext(context);
        contexts.saveContext(context,request,response);
        return SafeAccount.from(account(auth.getName()));
    }

    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        new SecurityContextLogoutHandler().logout(request,response,authentication); SecurityContextHolder.clearContext();
    }

    @PostMapping("/invites") @ResponseStatus(HttpStatus.CREATED) @Transactional
    InviteResponse createInvite(@RequestBody InviteRequest input) {
        if(input.role()!=AccountRole.DOCTOR) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose Doctor.");
        byte[] bytes=new byte[32]; new SecureRandom().nextBytes(bytes);
        String code=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); Instant expiry=Instant.now().plus(7,ChronoUnit.DAYS);
        RegistrationInvite invite=new RegistrationInvite(); invite.role=input.role(); invite.expiresAt=expiry; invite.tokenHash=hash(code); invites.save(invite);
        return new InviteResponse(code,input.role(),expiry);
    }

    private UserAccount account(String email) { return users.findByEmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Account not found.")); }
    private UserAccount newAccount(String email,String password,String name,AccountRole role,Long profileId) {
        UserAccount user=new UserAccount(); user.email=normalize(email); user.passwordHash=passwords.encode(password); user.fullName=name.trim(); user.role=role; user.profileId=profileId; return user;
    }
    private void authenticate(UserAccount user,HttpServletRequest request,HttpServletResponse response) {
        Authentication auth=new UsernamePasswordAuthenticationToken(user.email,null,
            java.util.List.of(new SimpleGrantedAuthority("ROLE_"+user.role.name())));
        sessionStrategy.onAuthentication(auth,request,response);
        SecurityContext context=SecurityContextHolder.createEmptyContext(); context.setAuthentication(auth);
        SecurityContextHolder.setContext(context); contexts.saveContext(context,request,response);
    }
    private void validateCommon(String email,String password,String name) {
        if(email==null || !email.trim().toLowerCase().matches("^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@gmail\\.com$"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use a valid @gmail.com address.");
        if(password==null || password.length()<10 || password.length()>72 || password.chars().noneMatch(Character::isLetter) || password.chars().noneMatch(Character::isDigit))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Password must be 10–72 characters and include a letter and a number.");
        if(blank(name) || name.length()>120) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter your name (up to 120 characters).");
    }
    private void ensureEmailAvailable(String email) { if(users.existsByEmailIgnoreCase(normalize(email))) throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with that Gmail already exists."); }
    private static String normalize(String value) { return value.trim().toLowerCase(); }
    private static boolean blank(String value) { return value==null || value.isBlank(); }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(Exception e) { throw new IllegalStateException(e); }
    }
}
