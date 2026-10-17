package dev.daniellavoie.opencabinet.invitation.domain.port.backend;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.daniellavoie.opencabinet.invitation.domain.model.Invitation;

public interface InvitationRepository {
	Invitation create(Invitation invitation, String tokenHash);

	List<Invitation> findPending(UUID shopId);

	Optional<Invitation> findByTokenHash(String tokenHash);

	/**
	 * Atomically marks an Invitation accepted; fails if it already was.
	 */
	void markAccepted(UUID invitationId);

	void delete(UUID invitationId, UUID shopId);
}
