package dev.daniellavoie.opencabinet.cutlist.domain;

import dev.daniellavoie.opencabinet.common.domain.Language;
import dev.daniellavoie.opencabinet.cutlist.domain.model.PartRole;

/**
 * What each Part is called on printed outputs, worded as in the app.
 */
public final class PartRoleNames {
	private PartRoleNames() {
	}

	public static String of(PartRole role, Language language) {
		return switch (role) {
		case SIDE -> language.pick("Côté", "Side");
		case BOTTOM -> language.pick("Fond", "Bottom");
		case TOP -> language.pick("Dessus", "Top");
		case STRETCHER -> language.pick("Traverse", "Stretcher");
		case BACK -> language.pick("Dos", "Back");
		case SHELF -> language.pick("Tablette", "Shelf");
		case DOOR -> language.pick("Porte", "Door");
		case DRAWER_FRONT -> language.pick("Façade de tiroir", "Drawer front");
		case FALSE_FRONT -> language.pick("Fausse façade", "False front");
		case BLIND_PANEL -> language.pick("Panneau d'angle aveugle", "Blind corner panel");
		case DIVIDER -> language.pick("Séparateur", "Divider");
		case FINISHED_END -> language.pick("Panneau de finition", "Finished end panel");
		case FILLER -> language.pick("Remplissage", "Filler");
		case TOE_KICK -> language.pick("Plinthe", "Toe kick");
		};
	}
}
