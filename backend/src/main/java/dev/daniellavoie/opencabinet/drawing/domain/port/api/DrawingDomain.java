package dev.daniellavoie.opencabinet.drawing.domain.port.api;

import java.security.Principal;
import java.util.UUID;

import dev.daniellavoie.opencabinet.common.domain.Language;
import dev.daniellavoie.opencabinet.drawing.domain.model.ShopDrawingSet;

public interface DrawingDomain {
	/**
	 * The Shop Drawings of a Project as saved: plan, an Elevation per Wall and a detail per Cabinet.
	 */
	ShopDrawingSet shopDrawings(UUID projectId, Language language, Principal principal);

	byte[] shopDrawingsPdf(UUID projectId, Language language, Principal principal);
}
