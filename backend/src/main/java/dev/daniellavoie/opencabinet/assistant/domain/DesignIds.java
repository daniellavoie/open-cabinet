package dev.daniellavoie.opencabinet.assistant.domain;

import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Where the ids of Walls, Cabinets and items a Design Proposal creates come from: random in the
 * app, sequential when a scenario is replayed, so its recorded tool calls name the same ids.
 */
@Component
public class DesignIds {
	public String next(String prefix) {
		return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
	}
}
