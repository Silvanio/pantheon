package com.pantheon.service.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pantheon.service.entity.ArchitecturalProject;
public interface ArchitecturalProjectRepository extends JpaRepository<ArchitecturalProject, UUID> {

    List<ArchitecturalProject> findByProjectId(UUID projectId);
}
