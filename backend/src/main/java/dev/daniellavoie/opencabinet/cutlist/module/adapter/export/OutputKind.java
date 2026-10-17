package dev.daniellavoie.opencabinet.cutlist.module.adapter.export;

import java.util.Arrays;
import java.util.NoSuchElementException;

import dev.daniellavoie.opencabinet.common.domain.Language;

/**
 * The outputs that can be printed or exported, by the path naming them.
 */
public enum OutputKind {
	CUT_LIST("cut-list"), NESTING("nesting"), BILL_OF_MATERIALS("bill-of-materials");

	private final String path;

	OutputKind(String path) {
		this.path = path;
	}

	public static OutputKind of(String path) {
		return Arrays.stream(values())
				.filter(kind -> kind.path.equals(path))
				.findFirst()
				.orElseThrow(() -> new NoSuchElementException("no output named " + path));
	}

	public String title(Language language) {
		return switch (this) {
		case CUT_LIST -> language.pick("Liste de coupe", "Cut list");
		case NESTING -> language.pick("Optimisation", "Nesting");
		case BILL_OF_MATERIALS -> language.pick("Matériaux", "Bill of materials");
		};
	}

	public String fileName(Language language) {
		return switch (this) {
		case CUT_LIST -> language.pick("liste-de-coupe", "cut-list");
		case NESTING -> language.pick("optimisation", "nesting");
		case BILL_OF_MATERIALS -> language.pick("materiaux", "bill-of-materials");
		};
	}
}
