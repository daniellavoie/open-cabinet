package dev.daniellavoie.opencabinet.drawing.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.daniellavoie.opencabinet.common.domain.Language;
import dev.daniellavoie.opencabinet.common.domain.Lengths;
import dev.daniellavoie.opencabinet.project.domain.ReadingOrder;
import dev.daniellavoie.opencabinet.project.domain.model.Appliance;
import dev.daniellavoie.opencabinet.project.domain.model.ApplianceKind;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.Filler;
import dev.daniellavoie.opencabinet.project.domain.model.Opening;
import dev.daniellavoie.opencabinet.project.domain.model.Wall;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

/**
 * What every drawing of a Design needs: the Design, its templates and Cabinet labels, the Shop's
 * toe kick height, front gap and units, and the language to write in.
 */
public record DrawingContext(Design design, Map<String, CabinetTemplate> templates, Map<String, String> labels,
		double toeKickHeightMm, double frontGapMm, Units units, Language language) {

	public static DrawingContext of(Design design, Map<String, CabinetTemplate> templates, double toeKickHeightMm,
			double frontGapMm, Units units, Language language) {
		return new DrawingContext(design, templates, ReadingOrder.labels(design), toeKickHeightMm, frontGapMm, units,
				language);
	}

	List<Wall> walls() {
		return Optional.ofNullable(design.getWalls()).orElse(List.of());
	}

	List<Cabinet> cabinets() {
		return ReadingOrder.cabinets(design);
	}

	List<Opening> openings() {
		return Optional.ofNullable(design.getOpenings()).orElse(List.of());
	}

	List<Appliance> appliances() {
		return Optional.ofNullable(design.getAppliances()).orElse(List.of());
	}

	List<Filler> fillers() {
		return Optional.ofNullable(design.getFillers()).orElse(List.of());
	}

	String length(double mm) {
		return Lengths.format(mm, units);
	}

	String label(Cabinet cabinet) {
		return labels.getOrDefault(cabinet.getId(), "");
	}

	String wallTitle(int index) {
		return language.pick("Mur ", "Wall ") + (index + 1);
	}

	String applianceName(ApplianceKind kind) {
		return switch (kind) {
		case FRIDGE -> language.pick("Réfrigérateur", "Fridge");
		case RANGE -> language.pick("Cuisinière", "Range");
		case DISHWASHER -> language.pick("Lave-vaisselle", "Dishwasher");
		case HOOD -> language.pick("Hotte", "Hood");
		};
	}

	String templateName(String code) {
		return switch (code) {
		case "BASE_DOOR" -> language.pick("Bas, portes", "Base, doors");
		case "BASE_DOOR_DRAWER" -> language.pick("Bas, tiroir et portes", "Base, drawer and doors");
		case "BASE_BLIND_CORNER" -> language.pick("Bas, coin aveugle", "Base, blind corner");
		case "BASE_SINK" -> language.pick("Bas d'évier", "Sink base");
		case "BASE_DRAWERS" -> language.pick("Bas, tiroirs", "Base, drawers");
		case "WALL_DOOR" -> language.pick("Haut, portes", "Wall, doors");
		case "TALL_PANTRY" -> language.pick("Colonne garde-manger", "Pantry tower");
		case "TALL_OVEN" -> language.pick("Colonne four", "Oven tower");
		default -> code;
		};
	}
}
