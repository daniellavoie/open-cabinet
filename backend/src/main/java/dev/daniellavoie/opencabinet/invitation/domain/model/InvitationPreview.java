package dev.daniellavoie.opencabinet.invitation.domain.model;

import dev.daniellavoie.opencabinet.member.domain.model.Role;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * What the invited person sees before accepting.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class InvitationPreview {
	String shopName;
	Role role;
	String invitedByName;
}
