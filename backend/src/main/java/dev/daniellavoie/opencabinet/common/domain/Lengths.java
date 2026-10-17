package dev.daniellavoie.opencabinet.common.domain;

import dev.daniellavoie.opencabinet.shop.domain.model.Units;

/**
 * Formats lengths the way the Shop measures, like {@code units.ts} in the browser: inches to the
 * nearest 1/16" (e.g. 34 1/2") or whole millimetres.
 */
public final class Lengths {
	public static final double MM_PER_INCH = 25.4;
	private static final int SIXTEENTHS = 16;

	private Lengths() {
	}

	public static String format(double mm, Units units) {
		if (units == Units.METRIC) {
			return Math.round(mm) + " mm";
		}

		var sign = mm < 0 ? "-" : "";
		var sixteenths = Math.round(Math.abs(mm) / MM_PER_INCH * SIXTEENTHS);
		var whole = sixteenths / SIXTEENTHS;
		var numerator = sixteenths % SIXTEENTHS;
		var denominator = SIXTEENTHS;
		while (numerator > 0 && numerator % 2 == 0) {
			numerator /= 2;
			denominator /= 2;
		}

		if (numerator == 0) {
			return sign + whole + "\"";
		}
		return whole == 0 ? sign + numerator + "/" + denominator + "\""
				: sign + whole + " " + numerator + "/" + denominator + "\"";
	}
}
