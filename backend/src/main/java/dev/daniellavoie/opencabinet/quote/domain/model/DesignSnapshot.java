package dev.daniellavoie.opencabinet.quote.domain.model;

import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * The design a Quote was priced from, with what its pictures need, so they never change after the
 * Quote is created.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class DesignSnapshot {
	Design design;
	double toeKickHeightMm;
	double frontGapMm;
	Units units;
}
