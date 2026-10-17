package dev.daniellavoie.opencabinet.invitation.domain.model;

import dev.daniellavoie.opencabinet.member.domain.model.Role;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class InvitationRequest {
	String email;
	Role role;
}
