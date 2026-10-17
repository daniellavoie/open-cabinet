package dev.daniellavoie.opencabinet.invitation.module.web;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.daniellavoie.opencabinet.invitation.domain.model.AcceptInvitationRequest;
import dev.daniellavoie.opencabinet.invitation.domain.model.CreatedInvitation;
import dev.daniellavoie.opencabinet.invitation.domain.model.Invitation;
import dev.daniellavoie.opencabinet.invitation.domain.model.InvitationPreview;
import dev.daniellavoie.opencabinet.invitation.domain.model.InvitationRequest;
import dev.daniellavoie.opencabinet.invitation.domain.port.api.InvitationDomain;
import dev.daniellavoie.opencabinet.member.domain.model.Member;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@AllArgsConstructor
@RequestMapping("/api/invitation")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvitationController {
	InvitationDomain domain;

	@PostMapping
	public CreatedInvitation create(@RequestBody InvitationRequest request, Principal principal) {
		return domain.create(request, principal);
	}

	@GetMapping
	public List<Invitation> findPending(Principal principal) {
		return domain.findPending(principal);
	}

	@DeleteMapping("/{invitationId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void revoke(@PathVariable UUID invitationId, Principal principal) {
		domain.revoke(invitationId, principal);
	}

	@GetMapping("/token/{token}")
	public InvitationPreview preview(@PathVariable String token) {
		return domain.preview(token);
	}

	@PostMapping("/token/{token}/accept")
	public Member accept(@PathVariable String token, @RequestBody AcceptInvitationRequest request,
			Principal principal) {
		return domain.accept(token, request, principal);
	}
}
