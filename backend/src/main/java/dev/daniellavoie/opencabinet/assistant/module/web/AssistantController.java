package dev.daniellavoie.opencabinet.assistant.module.web;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.daniellavoie.opencabinet.assistant.domain.model.AssistantStatus;
import dev.daniellavoie.opencabinet.assistant.domain.model.CheckReply;
import dev.daniellavoie.opencabinet.assistant.domain.model.UpdateAssistantSettingsRequest;
import dev.daniellavoie.opencabinet.assistant.domain.port.api.AssistantDomain;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@AllArgsConstructor
@RequestMapping("/api/assistant")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AssistantController {
	AssistantDomain domain;

	@GetMapping
	public AssistantStatus status(Principal principal) {
		return domain.status(principal);
	}

	@PutMapping
	public AssistantStatus updateSettings(@RequestBody UpdateAssistantSettingsRequest request, Principal principal) {
		return domain.updateSettings(request, principal);
	}

	@PostMapping("/check")
	public CheckReply check(Principal principal) {
		return domain.check(principal);
	}
}
