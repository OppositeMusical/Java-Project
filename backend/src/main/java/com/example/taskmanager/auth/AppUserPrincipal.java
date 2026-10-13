package com.example.taskmanager.auth;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The signed-in user as seen by Spring Security. Stored in the HTTP session, so it only
 * holds immutable, serializable values rather than the JPA entity.
 */
public record AppUserPrincipal(Long id, String email, String displayName, String passwordHash)
        implements UserDetails {

    private static final List<GrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    static AppUserPrincipal from(AppUser user) {
        return new AppUserPrincipal(user.getId(), user.getEmail(), user.getDisplayName(), user.getPasswordHash());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return AUTHORITIES;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String toString() {
        return "AppUserPrincipal[id=" + id + ", email=" + email + "]";
    }
}
