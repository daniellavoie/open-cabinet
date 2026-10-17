package dev.daniellavoie.opencabinet.invitation.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A new Invitation with its secret token, returned only once.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class CreatedInvitation {
	Invitation invitation;
	String token;
}
