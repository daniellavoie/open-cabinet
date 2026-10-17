package dev.daniellavoie.opencabinet.template.domain.port.backend;

import java.util.List;
import java.util.UUID;

import dev.daniellavoie.opencabinet.template.domain.model.ShopCabinetTemplate;

public interface ShopCabinetTemplateRepository {
	List<ShopCabinetTemplate> findByShop(UUID shopId);

	ShopCabinetTemplate save(ShopCabinetTemplate template, UUID shopId);

	void delete(UUID templateId, UUID shopId);
}
