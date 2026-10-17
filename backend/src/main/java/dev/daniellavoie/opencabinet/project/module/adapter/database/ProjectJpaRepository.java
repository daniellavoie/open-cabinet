package dev.daniellavoie.opencabinet.project.module.adapter.database;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.daniellavoie.opencabinet.project.module.adapter.database.entity.ProjectEntity;

public interface ProjectJpaRepository extends JpaRepository<ProjectEntity, UUID> {
	Page<ProjectEntity> findByShopId(UUID shopId, Pageable pageable);

	Optional<ProjectEntity> findByIdAndShopId(UUID id, UUID shopId);

	@Modifying
	@Query("""
			UPDATE Project p SET p.lockedBy = :memberId, p.lockExpirationTimestamp = :expiration
			WHERE p.id = :id AND p.shopId = :shopId
			AND (p.lockedBy IS NULL OR p.lockedBy = :memberId OR p.lockExpirationTimestamp < :now)""")
	int tryLock(@Param("id") UUID id, @Param("shopId") UUID shopId, @Param("memberId") UUID memberId,
			@Param("expiration") LocalDateTime expiration, @Param("now") LocalDateTime now);

	@Modifying
	@Query("""
			UPDATE Project p SET p.lockedBy = NULL, p.lockExpirationTimestamp = NULL
			WHERE p.id = :id AND p.shopId = :shopId AND p.lockedBy = :memberId""")
	int unlock(@Param("id") UUID id, @Param("shopId") UUID shopId, @Param("memberId") UUID memberId);
}
