package dev.daniellavoie.opencabinet.drawing.domain.port.api;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import dev.daniellavoie.opencabinet.drawing.domain.model.DrawingLanguage;
import dev.daniellavoie.opencabinet.drawing.domain.model.ShopDrawings;
import dev.daniellavoie.opencabinet.drawing.domain.model.View;
import dev.daniellavoie.opencabinet.project.domain.model.Project;
import dev.daniellavoie.opencabinet.shop.domain.model.ConstructionStandard;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

public interface ShopDrawingsDomain {
	/**
	 * The Shop Drawings of a Project as saved, in the Shop's units.
	 */
	ShopDrawings drawings(UUID projectId, DrawingLanguage language, Principal principal);

	/**
	 * The same, printable.
	 */
	byte[] pdf(UUID projectId, DrawingLanguage language, Principal principal);

	/**
	 * The pictures printed with a Quote of the Project: the Room in 3D and an Elevation of each Wall
	 * with Cabinets, in French like the Quote. None when the Project has no Cabinets.
	 */
	List<View> quotePictures(Project project, ConstructionStandard standard, Units units);
}
