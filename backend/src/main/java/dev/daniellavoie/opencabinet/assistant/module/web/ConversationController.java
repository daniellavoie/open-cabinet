package dev.daniellavoie.opencabinet.assistant.module.web;

import java.security.Principal;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.daniellavoie.opencabinet.assistant.domain.model.AcceptResult;
import dev.daniellavoie.opencabinet.assistant.domain.model.ConversationView;
import dev.daniellavoie.opencabinet.assistant.domain.model.DesignProposal;
import dev.daniellavoie.opencabinet.assistant.domain.model.SendMessageRequest;
import dev.daniellavoie.opencabinet.assistant.domain.port.api.ConversationDomain;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@AllArgsConstructor
@RequestMapping("/api/project/{projectId}")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationController {
	ConversationDomain domain;

	@GetMapping("/conversation")
	public ConversationView view(@PathVariable UUID projectId, @RequestParam(defaultValue = "0") int afterProgress,
			Principal principal) {
		return domain.view(projectId, afterProgress, principal);
	}

	@PostMapping("/conversation/message")
	public ConversationView send(@PathVariable UUID projectId, @RequestBody SendMessageRequest request, Principal principal) {
		return domain.send(projectId, request, principal);
	}

	@PostMapping("/proposal/{proposalId}/accept")
	public AcceptResult accept(@PathVariable UUID projectId, @PathVariable UUID proposalId, Principal principal) {
		return domain.accept(projectId, proposalId, principal);
	}

	@PostMapping("/proposal/{proposalId}/undo")
	public AcceptResult undoAccept(@PathVariable UUID projectId, @PathVariable UUID proposalId, Principal principal) {
		return domain.undoAccept(projectId, proposalId, principal);
	}

	@PostMapping("/proposal/{proposalId}/discard")
	public DesignProposal discard(@PathVariable UUID projectId, @PathVariable UUID proposalId, Principal principal) {
		return domain.discard(projectId, proposalId, principal);
	}
}
