package dev.daniellavoie.opencabinet.assistant.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import dev.daniellavoie.opencabinet.assistant.domain.model.ProgressEvent;

/**
 * What the assistant did for each Conversation's last request, kept in memory: the page polls it
 * while a request runs. A restart forgets it; the messages and Proposal stay.
 */
@Component
public class ConversationProgress {
	private final Map<UUID, List<ProgressEvent>> events = new ConcurrentHashMap<>();

	public void start(UUID conversationId) {
		events.put(conversationId, new ArrayList<>());
	}

	public void add(UUID conversationId, ProgressEvent.ProgressEventBuilder event) {
		var list = events.computeIfAbsent(conversationId, id -> new ArrayList<>());
		synchronized (list) {
			list.add(event.sequence(list.size() + 1).build());
		}
	}

	/**
	 * The events after {@code after}, oldest first.
	 */
	public List<ProgressEvent> since(UUID conversationId, int after) {
		var list = events.getOrDefault(conversationId, List.of());
		synchronized (list) {
			return list.stream().filter(event -> event.getSequence() > after).toList();
		}
	}
}
