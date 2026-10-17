package dev.daniellavoie.opencabinet.cutlist.domain.model;

import dev.daniellavoie.opencabinet.common.domain.Language;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

/**
 * A Project's output with what printing it needs: who it is for, the Shop's units and currency, and
 * the language to write in.
 */
public record OutputDocument(String shopName, String projectName, String clientName, Units units,
		String currency, Language language, ProjectOutput output) {
}
