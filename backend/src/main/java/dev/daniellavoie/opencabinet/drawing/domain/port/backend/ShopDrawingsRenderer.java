package dev.daniellavoie.opencabinet.drawing.domain.port.backend;

import dev.daniellavoie.opencabinet.drawing.domain.model.ShopDrawings;

public interface ShopDrawingsRenderer {
	/**
	 * Prints each Sheet on its own page, at a standard scale.
	 */
	byte[] pdf(ShopDrawings drawings);
}
