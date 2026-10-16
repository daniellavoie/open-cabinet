package dev.daniellavoie.opencabinet.shop.domain.port.api;

import java.security.Principal;

import dev.daniellavoie.opencabinet.shop.domain.model.CreateShopRequest;
import dev.daniellavoie.opencabinet.shop.domain.model.Shop;
import dev.daniellavoie.opencabinet.shop.domain.model.UpdateShopRequest;

public interface ShopDomain {
	Shop create(CreateShopRequest request, Principal principal);

	Shop current(Principal principal);

	Shop update(UpdateShopRequest request, Principal principal);

	int nextQuoteNumber(Principal principal);
}
