package com.pantheon.service.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pantheon.service.entity.Project;
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByCreatedBy(UUID createdBy);
}
