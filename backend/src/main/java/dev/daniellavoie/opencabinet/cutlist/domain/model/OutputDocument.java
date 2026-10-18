package dev.daniellavoie.opencabinet.cutlist.domain.model;

import java.util.Arrays;

/**
 * What can be printed or exported from a Project's output.
 */
public enum OutputDocument {
	CUT_LIST("cut-list"), NESTING("nesting"), BILL_OF_MATERIALS("bill-of-materials");

	private final String slug;

	OutputDocument(String slug) {
		this.slug = slug;
	}

	/**
	 * The document's name in URLs, e.g. cut-list.
	 */
	public String slug() {
		return slug;
	}

	public static OutputDocument of(String slug) {
		return Arrays.stream(values()).filter(document -> document.slug.equals(slug)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("unknown output document: " + slug));
	}
}
