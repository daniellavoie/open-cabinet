package dev.daniellavoie.opencabinet.common.domain;

import java.util.Locale;

/**
 * The language a printed output is written in: the one the Member reads the app in.
 */
public enum Language {
	FR(Locale.CANADA_FRENCH), EN(Locale.CANADA);

	private final Locale locale;

	Language(Locale locale) {
		this.locale = locale;
	}

	public Locale locale() {
		return locale;
	}

	/**
	 * French unless {@code code} asks for English.
	 */
	public static Language of(String code) {
		return code != null && code.toLowerCase(Locale.ROOT).startsWith("en") ? EN : FR;
	}

	/**
	 * Picks the French or English wording.
	 */
	public String pick(String french, String english) {
		return this == EN ? english : french;
	}
}
