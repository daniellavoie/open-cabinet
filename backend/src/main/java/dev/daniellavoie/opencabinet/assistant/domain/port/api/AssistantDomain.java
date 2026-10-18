package dev.daniellavoie.opencabinet.assistant.domain.port.api;

import java.security.Principal;

import dev.daniellavoie.opencabinet.assistant.domain.model.AssistantStatus;
import dev.daniellavoie.opencabinet.assistant.domain.model.CheckReply;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelReply;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest;
import dev.daniellavoie.opencabinet.assistant.domain.model.UpdateAssistantSettingsRequest;

public interface AssistantDomain {
	AssistantStatus status(Principal principal);

	/**
	 * Opts the Shop in or out and sets its monthly budget. Owners only.
	 */
	AssistantStatus updateSettings(UpdateAssistantSettingsRequest request, Principal principal);

	/**
	 * Sends a short test request, counted against the budget. Owners only.
	 */
	CheckReply check(Principal principal);

	/**
	 * Asks the model for the current Member's Shop: refused unless the Shop opted in and has budget
	 * left this month; the cost is recorded.
	 */
	ModelReply complete(ModelRequest request, Principal principal);
}
