package com.example.taskmanager.project;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record ProjectRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 1000) String description) {
    }

    public record ProjectResponse(
            Long id,
            String name,
            String description,
            long taskCount,
            long openTaskCount,
            OffsetDateTime createdAt) {
    }
}
