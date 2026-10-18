package dev.daniellavoie.opencabinet.assistant.module.adapter.database;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.daniellavoie.opencabinet.assistant.domain.model.ProposalStatus;
import dev.daniellavoie.opencabinet.assistant.module.adapter.database.entity.DesignProposalEntity;

public interface DesignProposalJpaRepository extends JpaRepository<DesignProposalEntity, UUID> {
	Optional<DesignProposalEntity> findFirstByProjectIdAndShopIdAndStatusOrderByUpdateTimestampDesc(UUID projectId, UUID shopId,
			ProposalStatus status);

	Optional<DesignProposalEntity> findByIdAndShopId(UUID id, UUID shopId);
}
