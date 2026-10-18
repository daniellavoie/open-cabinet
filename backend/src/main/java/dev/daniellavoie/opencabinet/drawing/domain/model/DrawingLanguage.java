package dev.daniellavoie.opencabinet.drawing.domain.model;

import java.util.Locale;

import dev.daniellavoie.opencabinet.catalog.domain.model.MaterialKind;
import dev.daniellavoie.opencabinet.project.domain.model.PartRole;
import dev.daniellavoie.opencabinet.project.domain.model.ApplianceKind;
import dev.daniellavoie.opencabinet.project.domain.model.OpeningKind;

/**
 * The language the Shop Drawings are printed in: the Member's, French unless English is asked
 * for. Names match the app's ({@code labels.ts}).
 */
public enum DrawingLanguage {
	FR(Locale.CANADA_FRENCH), EN(Locale.CANADA);

	private final Locale locale;

	DrawingLanguage(Locale locale) {
		this.locale = locale;
	}

	public static DrawingLanguage of(String code) {
		return "en".equalsIgnoreCase(code) ? EN : FR;
	}

	public Locale locale() {
		return locale;
	}

	public String pick(String french, String english) {
		return this == FR ? french : english;
	}

	public String templateName(String code) {
		return switch (code) {
		case "BASE_DOOR" -> pick("Bas, portes", "Base, doors");
		case "BASE_DOOR_DRAWER" -> pick("Bas, tiroir et portes", "Base, drawer and doors");
		case "BASE_BLIND_CORNER" -> pick("Bas, coin aveugle", "Base, blind corner");
		case "TALL_PANTRY" -> pick("Colonne garde-manger", "Tall pantry");
		case "TALL_OVEN" -> pick("Colonne four", "Oven tower");
		case "BASE_SINK" -> pick("Bas d'évier", "Sink base");
		case "BASE_DRAWERS" -> pick("Bas, tiroirs", "Base, drawers");
		case "WALL_DOOR" -> pick("Haut, portes", "Wall, doors");
		default -> code;
		};
	}

	public String partRole(PartRole role) {
		return switch (role) {
		case SIDE -> pick("Côté", "Side");
		case BOTTOM -> pick("Fond", "Bottom");
		case TOP -> pick("Dessus", "Top");
		case STRETCHER -> pick("Traverse", "Stretcher");
		case BACK -> pick("Dos", "Back");
		case SHELF -> pick("Tablette", "Shelf");
		case DOOR -> pick("Porte", "Door");
		case DRAWER_FRONT -> pick("Façade de tiroir", "Drawer front");
		case FALSE_FRONT -> pick("Fausse façade", "False front");
		case BLIND_PANEL -> pick("Panneau d'angle aveugle", "Blind corner panel");
		case DIVIDER -> pick("Séparateur", "Divider");
		case FINISHED_END -> pick("Panneau de finition", "Finished end");
		case FILLER -> pick("Remplissage", "Filler");
		case TOE_KICK -> pick("Plinthe", "Toe kick");
		};
	}

	public String opening(OpeningKind kind) {
		return switch (kind) {
		case WINDOW -> pick("Fenêtre", "Window");
		case DOORWAY -> pick("Porte", "Doorway");
		};
	}

	public String appliance(ApplianceKind kind) {
		return switch (kind) {
		case FRIDGE -> pick("Réfrigérateur", "Fridge");
		case RANGE -> pick("Cuisinière", "Range");
		case DISHWASHER -> pick("Lave-vaisselle", "Dishwasher");
		case HOOD -> pick("Hotte", "Hood");
		};
	}

	public String materialKind(MaterialKind kind) {
		return switch (kind) {
		case SHEET -> pick("Panneau", "Sheet");
		case EDGE_BANDING -> pick("Bande de chant", "Edge banding");
		case HARDWARE -> pick("Quincaillerie", "Hardware");
		case FRONT -> pick("Façades commandées", "Ordered fronts");
		case MOULDING -> pick("Moulures", "Mouldings");
		};
	}

	public String wall(int number) {
		return pick("Mur ", "Wall ") + number;
	}
}
