package com.example.taskmanager.project;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByOwnerIdOrderByNameAsc(Long ownerId);

    Optional<Project> findByIdAndOwnerId(Long id, Long ownerId);
}
