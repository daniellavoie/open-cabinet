package dev.daniellavoie.opencabinet.assistant.domain.port.backend;

/**
 * Runs the assistant's requests in the background, so a Member's message returns at once.
 */
public interface AssistantJobs {
	void run(Runnable job);
}
