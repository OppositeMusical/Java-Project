package com.example.taskmanager.task;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class TaskDtos {

    private TaskDtos() {
    }

    public record TaskRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            TaskStatus status,
            TaskPriority priority,
            LocalDate dueDate,
            @NotNull Long projectId) {
    }

    public record StatusUpdateRequest(@NotNull TaskStatus status) {
    }

    public record TaskResponse(
            Long id,
            String title,
            String description,
            TaskStatus status,
            TaskPriority priority,
            LocalDate dueDate,
            Long projectId,
            String projectName,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {

        static TaskResponse from(Task task) {
            return new TaskResponse(
                    task.getId(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getStatus(),
                    task.getPriority(),
                    task.getDueDate(),
                    task.getProject().getId(),
                    task.getProject().getName(),
                    task.getCreatedAt(),
                    task.getUpdatedAt());
        }
    }
}
