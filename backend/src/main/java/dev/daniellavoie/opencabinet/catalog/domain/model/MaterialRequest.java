package dev.daniellavoie.opencabinet.catalog.domain.model;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class MaterialRequest {
	MaterialKind kind;
	String name;
	String sku;
	BigDecimal price;
	Double thicknessMm;
	Double lengthMm;
	Double widthMm;
	boolean grain;
}
