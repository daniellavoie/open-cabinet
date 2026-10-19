package dev.daniellavoie.opencabinet.project.domain.model;

import java.util.List;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Where a design does not follow a Design Guideline: the guideline's {@code code}, the Wall and the
 * Cabinets or items involved, and, when it is about a length, what it measures and the limit it
 * misses. Unlike a {@link Violation}, it never blocks.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class Remark {
	String code;
	String wallId;
	List<String> ids;
	Double measureMm;
	Double limitMm;

	/**
	 * Names this remark among a design's remarks: the same guideline at the same place.
	 */
	public String key() {
		return code + ":" + String.join(",", ids == null ? List.of() : ids);
	}
}
