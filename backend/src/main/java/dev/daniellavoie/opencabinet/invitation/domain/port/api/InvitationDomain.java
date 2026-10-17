package dev.daniellavoie.opencabinet.invitation.domain.port.api;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import dev.daniellavoie.opencabinet.invitation.domain.model.AcceptInvitationRequest;
import dev.daniellavoie.opencabinet.invitation.domain.model.CreatedInvitation;
import dev.daniellavoie.opencabinet.invitation.domain.model.Invitation;
import dev.daniellavoie.opencabinet.invitation.domain.model.InvitationPreview;
import dev.daniellavoie.opencabinet.invitation.domain.model.InvitationRequest;
import dev.daniellavoie.opencabinet.member.domain.model.Member;

public interface InvitationDomain {
	CreatedInvitation create(InvitationRequest request, Principal principal);

	/**
	 * Invitations of the Owner's Shop that are neither accepted nor expired.
	 */
	List<Invitation> findPending(Principal principal);

	void revoke(UUID invitationId, Principal principal);

	InvitationPreview preview(String token);

	Member accept(String token, AcceptInvitationRequest request, Principal principal);
}
