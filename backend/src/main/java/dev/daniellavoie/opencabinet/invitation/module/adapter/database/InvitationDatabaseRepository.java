package dev.daniellavoie.opencabinet.invitation.module.adapter.database;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import dev.daniellavoie.opencabinet.invitation.domain.model.Invitation;
import dev.daniellavoie.opencabinet.invitation.domain.port.backend.InvitationRepository;
import dev.daniellavoie.opencabinet.invitation.module.adapter.database.entity.InvitationEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvitationDatabaseRepository implements InvitationRepository {
	InvitationJpaRepository jpaRepository;

	@Override
	@Transactional
	public Invitation create(Invitation invitation, String tokenHash) {
		log.info("Creating invitation {} with: { shopId = {}, role = {} }", invitation.getId(), invitation.getShopId(),
				invitation.getRole());

		return jpaRepository.save(InvitationEntity.builder()
				.id(invitation.getId())
				.shopId(invitation.getShopId())
				.tokenHash(tokenHash)
				.email(invitation.getEmail())
				.role(invitation.getRole())
				.invitedBy(invitation.getInvitedBy())
				.expirationTimestamp(invitation.getExpirationTimestamp())
				.creationTimestamp(invitation.getCreationTimestamp())
				.build())
				.to();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Invitation> findPending(UUID shopId) {
		return jpaRepository
				.findByShopIdAndAcceptanceTimestampIsNullAndExpirationTimestampAfterOrderByCreationTimestampDesc(shopId,
						LocalDateTime.now())
				.stream()
				.map(InvitationEntity::to)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Invitation> findByTokenHash(String tokenHash) {
		return jpaRepository.findByTokenHash(tokenHash).map(InvitationEntity::to);
	}

	@Override
	@Transactional
	public void markAccepted(UUID invitationId) {
		if (jpaRepository.markAccepted(invitationId, LocalDateTime.now()) == 0) {
			throw new NoSuchElementException("this invitation is invalid or has expired");
		}
	}

	@Override
	@Transactional
	public void delete(UUID invitationId, UUID shopId) {
		jpaRepository.delete(jpaRepository.findByIdAndShopId(invitationId, shopId)
				.orElseThrow(() -> new NoSuchElementException("invitation " + invitationId + " not found")));
	}
}
