package dev.daniellavoie.opencabinet.assistant.domain.port.api;

import java.security.Principal;
import java.util.UUID;

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

	/**
	 * The same, for a request a Member started and a background job carries on.
	 */
	ModelReply complete(ModelRequest request, UUID shopId, UUID memberId);

	/**
	 * Whether the Shop opted in and the server can reach the model.
	 */
	boolean isAvailable(UUID shopId);
}
