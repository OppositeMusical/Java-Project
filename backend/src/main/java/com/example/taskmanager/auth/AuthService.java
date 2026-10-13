package com.example.taskmanager.auth;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanager.auth.AuthDtos.RegisterRequest;
import com.example.taskmanager.common.ConflictException;

@Service
public class AuthService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUserPrincipal register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        AppUser user = users.save(new AppUser(
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().strip()));
        return AppUserPrincipal.from(user);
    }

    static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
