package dev.daniellavoie.opencabinet.cutlist.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

import dev.daniellavoie.opencabinet.catalog.domain.model.MaterialKind;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * One thing to buy. {@code quantity} is in sheets, metres of edge banding, hardware units or square
 * metres of ordered Fronts.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class BomLine {
	UUID materialId;
	MaterialKind kind;
	String name;
	String sku;
	BigDecimal quantity;
	BigDecimal unitPrice;
	BigDecimal total;
}
