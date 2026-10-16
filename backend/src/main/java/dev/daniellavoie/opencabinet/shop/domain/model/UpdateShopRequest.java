package dev.daniellavoie.opencabinet.shop.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class UpdateShopRequest {
	String name;
	String currency;
	Units units;
	ShopSettings settings;
}
