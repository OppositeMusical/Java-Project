package com.example.taskmanager.auth;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public AppUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public AppUserPrincipal loadUserByUsername(String email) {
        return users.findByEmail(AuthService.normalizeEmail(email))
                .map(AppUserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("No user with that email"));
    }
}
