package dev.daniellavoie.opencabinet.template.domain.port.api;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import dev.daniellavoie.opencabinet.template.domain.model.ShopCabinetTemplate;
import dev.daniellavoie.opencabinet.template.domain.model.ShopCabinetTemplateRequest;

public interface ShopCabinetTemplateDomain {
	List<ShopCabinetTemplate> findAll(Principal principal);

	/**
	 * Saves a designed Cabinet as a template. Owners only.
	 */
	ShopCabinetTemplate create(ShopCabinetTemplateRequest request, Principal principal);

	/**
	 * Owners only. Cabinets already added from the template are unaffected.
	 */
	void delete(UUID templateId, Principal principal);
}
