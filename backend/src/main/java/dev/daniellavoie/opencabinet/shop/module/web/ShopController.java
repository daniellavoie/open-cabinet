package dev.daniellavoie.opencabinet.shop.module.web;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.daniellavoie.opencabinet.shop.domain.model.CreateShopRequest;
import dev.daniellavoie.opencabinet.shop.domain.model.Shop;
import dev.daniellavoie.opencabinet.shop.domain.model.UpdateShopRequest;
import dev.daniellavoie.opencabinet.shop.domain.port.api.ShopDomain;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@AllArgsConstructor
@RequestMapping("/api/shop")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShopController {
	ShopDomain domain;

	@PostMapping
	public Shop create(@RequestBody CreateShopRequest request, Principal principal) {
		return domain.create(request, principal);
	}

	@GetMapping
	public Shop current(Principal principal) {
		return domain.current(principal);
	}

	@PutMapping
	public Shop update(@RequestBody UpdateShopRequest request, Principal principal) {
		return domain.update(request, principal);
	}
}
