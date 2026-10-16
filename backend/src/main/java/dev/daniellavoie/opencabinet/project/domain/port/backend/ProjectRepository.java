package dev.daniellavoie.opencabinet.project.domain.port.backend;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dev.daniellavoie.opencabinet.project.domain.model.Project;

public interface ProjectRepository {
	Page<Project> findByShop(UUID shopId, Pageable pageable);

	Project findById(UUID projectId, UUID shopId);

	Project save(Project project, UUID shopId);
}
