package dev.daniellavoie.opencabinet.shop.domain.model;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class ShopSettings {
	BigDecimal markupPercent;
	List<Tax> taxes;
	LabourSettings labour;
	ConstructionStandard constructionStandard;
}
