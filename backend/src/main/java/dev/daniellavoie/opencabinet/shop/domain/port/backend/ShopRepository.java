package dev.daniellavoie.opencabinet.shop.domain.port.backend;

import java.util.UUID;

import dev.daniellavoie.opencabinet.shop.domain.model.Shop;

public interface ShopRepository {
	Shop create(Shop shop);

	Shop findById(UUID shopId);

	Shop update(Shop shop);

	int nextQuoteNumber(UUID shopId);
}
