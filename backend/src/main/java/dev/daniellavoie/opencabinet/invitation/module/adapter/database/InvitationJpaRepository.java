package dev.daniellavoie.opencabinet.invitation.module.adapter.database;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.daniellavoie.opencabinet.invitation.module.adapter.database.entity.InvitationEntity;

public interface InvitationJpaRepository extends JpaRepository<InvitationEntity, UUID> {
	List<InvitationEntity> findByShopIdAndAcceptanceTimestampIsNullAndExpirationTimestampAfterOrderByCreationTimestampDesc(
			UUID shopId, LocalDateTime now);

	Optional<InvitationEntity> findByTokenHash(String tokenHash);

	Optional<InvitationEntity> findByIdAndShopId(UUID id, UUID shopId);

	/**
	 * Claims an Invitation; returns 0 when someone else already did.
	 */
	@Modifying
	@Query("UPDATE Invitation i SET i.acceptanceTimestamp = :now WHERE i.id = :id AND i.acceptanceTimestamp IS NULL")
	int markAccepted(@Param("id") UUID id, @Param("now") LocalDateTime now);
}
