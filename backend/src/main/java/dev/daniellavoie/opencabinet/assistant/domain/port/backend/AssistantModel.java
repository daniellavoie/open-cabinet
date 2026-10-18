package dev.daniellavoie.opencabinet.assistant.domain.port.backend;

import dev.daniellavoie.opencabinet.assistant.domain.model.ModelReply;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelTier;

/**
 * The language model behind the Design Assistant. Only adapters know the provider.
 */
public interface AssistantModel {
	/**
	 * Whether the server has what it needs to reach the model, such as an API key.
	 */
	boolean isConfigured();

	String modelFor(ModelTier tier);

	ModelReply complete(ModelRequest request);
}
