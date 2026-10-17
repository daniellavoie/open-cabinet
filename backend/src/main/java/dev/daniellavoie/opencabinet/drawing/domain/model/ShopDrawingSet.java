package dev.daniellavoie.opencabinet.drawing.domain.model;

import java.util.List;

import dev.daniellavoie.opencabinet.common.domain.Language;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

/**
 * A Project's Shop Drawings, ready to print.
 */
public record ShopDrawingSet(String shopName, String projectName, String clientName, Units units,
		Language language, List<ShopDrawingSheet> sheets) {
}
