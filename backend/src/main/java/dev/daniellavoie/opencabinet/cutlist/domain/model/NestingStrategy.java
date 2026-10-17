package dev.daniellavoie.opencabinet.cutlist.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * How a layout was found: which way the first rip cuts run, which Parts go first, and which way
 * Parts without grain lie.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class NestingStrategy {
	public enum StripDirection {
		/**
		 * Strips run the sheet's length (and grain): ripped first, then cross-cut.
		 */
		ALONG,
		/**
		 * Strips run across the sheet: cross-cut first, then ripped.
		 */
		ACROSS
	}

	public enum Order {
		/**
		 * Widest first, so each strip is set by its widest Part.
		 */
		WIDTH,
		LENGTH,
		AREA
	}

	public enum Orientation {
		/**
		 * Parts without grain lie with their long side along the sheet.
		 */
		LONG_ALONG,
		LONG_ACROSS
	}

	StripDirection stripDirection;
	Order order;
	Orientation orientation;
}
