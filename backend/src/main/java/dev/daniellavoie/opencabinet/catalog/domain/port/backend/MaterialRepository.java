package dev.daniellavoie.opencabinet.catalog.domain.port.backend;

import java.util.List;
import java.util.UUID;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;

public interface MaterialRepository {
	List<Material> findByShop(UUID shopId);

	Material findById(UUID materialId, UUID shopId);

	Material save(Material material, UUID shopId);

	/**
	 * Saves every material in one transaction.
	 */
	List<Material> saveAll(List<Material> materials, UUID shopId);

	void delete(UUID materialId, UUID shopId);
}
