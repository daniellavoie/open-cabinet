package dev.daniellavoie.opencabinet.project.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A broken Construction Rule. {@code code} is shared with the front-end implementation. It names
 * the Cabinet, or for Openings and Appliances the {@code itemId}, that breaks it.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class Violation {
	String code;
	String wallId;
	String cabinetId;
	String itemId;
}
