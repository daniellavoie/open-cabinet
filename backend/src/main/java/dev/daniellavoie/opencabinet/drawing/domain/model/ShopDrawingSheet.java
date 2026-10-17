package dev.daniellavoie.opencabinet.drawing.domain.model;

import java.util.List;

import dev.daniellavoie.opencabinet.cutlist.domain.model.CutListRow;

/**
 * One page of the Shop Drawings: a drawing and, for a Cabinet detail, that Cabinet's Parts.
 */
public record ShopDrawingSheet(Kind kind, Drawing drawing, List<CutListRow> parts) {
	public enum Kind {
		PLAN, ELEVATION, CABINET
	}
}
