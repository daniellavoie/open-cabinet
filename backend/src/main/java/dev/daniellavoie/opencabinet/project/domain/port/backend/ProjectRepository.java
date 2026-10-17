package dev.daniellavoie.opencabinet.project.domain.port.backend;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dev.daniellavoie.opencabinet.project.domain.model.Project;

public interface ProjectRepository {
	Page<Project> findByShop(UUID shopId, Pageable pageable);

	Project findById(UUID projectId, UUID shopId);

	Project save(Project project, UUID shopId);

	/**
	 * Atomically gives the lock to {@code memberId} until {@code expiration} if it is free, lapsed or
	 * already theirs. Returns whether it did.
	 */
	boolean tryLock(UUID projectId, UUID shopId, UUID memberId, LocalDateTime expiration);

	void unlock(UUID projectId, UUID shopId, UUID memberId);

	/**
	 * The current, unexpired lock holder and expiration, if any.
	 */
	Optional<LockState> lockState(UUID projectId, UUID shopId);

	record LockState(UUID holderId, LocalDateTime expirationTimestamp) {
	}
}
