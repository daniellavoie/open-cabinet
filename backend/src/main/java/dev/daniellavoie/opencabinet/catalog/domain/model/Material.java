package dev.daniellavoie.opencabinet.catalog.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * One entry of a Shop's Material Catalog. Sheet dimensions are in millimetres; for grained sheets
 * the grain runs along the length.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class Material {
	UUID id;
	MaterialKind kind;
	String name;
	String sku;
	BigDecimal price;
	Double thicknessMm;
	Double lengthMm;
	Double widthMm;
	boolean grain;
}
