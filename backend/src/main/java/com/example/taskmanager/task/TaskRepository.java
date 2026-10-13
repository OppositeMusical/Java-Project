package com.example.taskmanager.task;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("""
            select t from Task t join fetch t.project p
            where p.owner.id = :ownerId
              and (:projectId is null or p.id = :projectId)
              and (:status is null or t.status = :status)
            order by t.createdAt desc, t.id desc
            """)
    List<Task> search(
            @Param("ownerId") Long ownerId,
            @Param("projectId") Long projectId,
            @Param("status") TaskStatus status);

    Optional<Task> findByIdAndProjectOwnerId(Long id, Long ownerId);

    long countByProjectId(Long projectId);

    long countByProjectIdAndStatus(Long projectId, TaskStatus status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Task t where t.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);
}
