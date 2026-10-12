package com.clinic.management;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
class AuthController {
    private final UserAccountRepository users;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contexts = new HttpSessionSecurityContextRepository();
    private final ChangeSessionIdAuthenticationStrategy sessionStrategy = new ChangeSessionIdAuthenticationStrategy();

    AuthController(UserAccountRepository users, AuthenticationManager authenticationManager) {
        this.users=users;
        this.authenticationManager=authenticationManager;
    }

    @GetMapping("/me") SafeAccount me(Authentication authentication) {
        return SafeAccount.from(account(authentication.getName()));
    }

    @PostMapping("/login") SafeAccount login(@RequestBody LoginRequest input, HttpServletRequest request, HttpServletResponse response) {
        if(input.email()==null || input.password()==null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter your Gmail and password.");
        Authentication auth=authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(normalize(input.email()),input.password()));
        sessionStrategy.onAuthentication(auth,request,response);
        SecurityContext context=SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context,request,response);
        return SafeAccount.from(account(auth.getName()));
    }

    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        new SecurityContextLogoutHandler().logout(request,response,authentication);
        SecurityContextHolder.clearContext();
    }

    private UserAccount account(String email) {
        return users.findByEmailIgnoreCase(email)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Account not found."));
    }

    private static String normalize(String value) { return value.trim().toLowerCase(); }
}
