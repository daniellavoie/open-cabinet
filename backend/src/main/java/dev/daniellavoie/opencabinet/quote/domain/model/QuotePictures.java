package dev.daniellavoie.opencabinet.quote.domain.model;

import java.util.List;

import dev.daniellavoie.opencabinet.drawing.domain.model.Drawing;

/**
 * What the Client sees of the design: a 3D view, the plan and an Elevation per Wall.
 */
public record QuotePictures(Drawing view, Drawing plan, List<Drawing> elevations) {
	public static final QuotePictures NONE = new QuotePictures(null, null, List.of());

	public boolean isEmpty() {
		return view == null;
	}
}
