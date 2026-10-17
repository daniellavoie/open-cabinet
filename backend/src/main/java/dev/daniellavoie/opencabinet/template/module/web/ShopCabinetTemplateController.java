package dev.daniellavoie.opencabinet.template.module.web;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.daniellavoie.opencabinet.template.domain.model.ShopCabinetTemplate;
import dev.daniellavoie.opencabinet.template.domain.model.ShopCabinetTemplateRequest;
import dev.daniellavoie.opencabinet.template.domain.port.api.ShopCabinetTemplateDomain;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@AllArgsConstructor
@RequestMapping("/api/shop-template")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShopCabinetTemplateController {
	ShopCabinetTemplateDomain domain;

	@GetMapping
	public List<ShopCabinetTemplate> findAll(Principal principal) {
		return domain.findAll(principal);
	}

	@PostMapping
	public ShopCabinetTemplate create(@RequestBody ShopCabinetTemplateRequest request, Principal principal) {
		return domain.create(request, principal);
	}

	@DeleteMapping("/{templateId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID templateId, Principal principal) {
		domain.delete(templateId, principal);
	}
}
