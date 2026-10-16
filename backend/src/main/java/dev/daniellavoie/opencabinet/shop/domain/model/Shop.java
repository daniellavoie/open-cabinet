package dev.daniellavoie.opencabinet.shop.domain.model;

import java.util.UUID;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class Shop {
	UUID id;
	String name;
	String currency;
	Units units;
	ShopSettings settings;
}
