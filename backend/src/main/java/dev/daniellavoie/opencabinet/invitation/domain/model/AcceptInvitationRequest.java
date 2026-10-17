package dev.daniellavoie.opencabinet.invitation.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class AcceptInvitationRequest {
	String memberName;
	String memberEmail;
}
