package dev.daniellavoie.opencabinet.drawing.domain.port.backend;

import dev.daniellavoie.opencabinet.drawing.domain.model.ShopDrawingSet;

public interface ShopDrawingsRenderer {
	byte[] pdf(ShopDrawingSet set);
}
