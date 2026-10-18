package dev.daniellavoie.opencabinet.drawing.domain;

import dev.daniellavoie.opencabinet.shop.domain.model.Units;

/**
 * Writes lengths the way the Shop measures, like the app's {@code formatLength}: inches to the
 * nearest 1/16" (34 1/2") or whole millimetres.
 */
public final class LengthFormat {
	static final double MM_PER_INCH = 25.4;
	private static final int SIXTEENTHS = 16;

	private LengthFormat() {
	}

	public static String format(double mm, Units units) {
		if (units == Units.METRIC) {
			return Math.round(mm) + " mm";
		}

		var sign = mm < 0 ? "-" : "";
		var sixteenths = Math.round(Math.abs(mm) / MM_PER_INCH * SIXTEENTHS);
		var whole = sixteenths / SIXTEENTHS;
		var numerator = sixteenths % SIXTEENTHS;
		var denominator = (long) SIXTEENTHS;
		while (numerator > 0 && numerator % 2 == 0) {
			numerator /= 2;
			denominator /= 2;
		}

		if (numerator == 0) {
			return sign + whole + "\"";
		}
		return whole == 0
				? sign + numerator + "/" + denominator + "\""
				: sign + whole + " " + numerator + "/" + denominator + "\"";
	}
}
