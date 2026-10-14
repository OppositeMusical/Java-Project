package com.example.taskmanager.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 255) String email,
            // BCrypt rejects passwords over 72 bytes, so limit bytes, not characters.
            @NotBlank @Size(min = 8) @MaxUtf8Bytes(72) String password,
            @NotBlank @Size(max = 100) String displayName) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record UserResponse(Long id, String email, String displayName) {

        static UserResponse from(AppUserPrincipal principal) {
            return new UserResponse(principal.id(), principal.email(), principal.displayName());
        }
    }
}
