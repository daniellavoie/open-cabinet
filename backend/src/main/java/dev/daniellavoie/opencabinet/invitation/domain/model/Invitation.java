package dev.daniellavoie.opencabinet.invitation.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import dev.daniellavoie.opencabinet.member.domain.model.Role;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A pending offer to join a Shop with a given role. Only a hash of its token is kept, so the link
 * is shown once, when the Invitation is created.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class Invitation {
	UUID id;
	UUID shopId;
	String email;
	Role role;
	UUID invitedBy;
	LocalDateTime expirationTimestamp;
	LocalDateTime acceptanceTimestamp;
	LocalDateTime creationTimestamp;
}
