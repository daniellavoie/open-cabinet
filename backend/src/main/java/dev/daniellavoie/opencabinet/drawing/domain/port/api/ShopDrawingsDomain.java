package dev.daniellavoie.opencabinet.drawing.domain.port.api;

import java.security.Principal;
import java.util.UUID;

import dev.daniellavoie.opencabinet.drawing.domain.model.DrawingLanguage;
import dev.daniellavoie.opencabinet.drawing.domain.model.ShopDrawings;

public interface ShopDrawingsDomain {
	/**
	 * The Shop Drawings of a Project as saved, in the Shop's units.
	 */
	ShopDrawings drawings(UUID projectId, DrawingLanguage language, Principal principal);

	/**
	 * The same, printable.
	 */
	byte[] pdf(UUID projectId, DrawingLanguage language, Principal principal);
}
