package dev.daniellavoie.opencabinet.catalog.domain.port.api;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import dev.daniellavoie.opencabinet.catalog.domain.model.CatalogImportResult;
import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.catalog.domain.model.MaterialRequest;
import dev.daniellavoie.opencabinet.shop.domain.model.ConstructionStandard;

public interface CatalogDomain {
	List<Material> findAll(Principal principal);

	List<Material> findByShop(UUID shopId);

	Material create(MaterialRequest request, Principal principal);

	Material update(UUID materialId, MaterialRequest request, Principal principal);

	void delete(UUID materialId, Principal principal);

	String exportCsv(Principal principal);

	/**
	 * Creates or updates (by SKU) every material in a CSV file. All or nothing: with any error,
	 * nothing changes and every error is reported.
	 */
	CatalogImportResult importCsv(String csv, Principal principal);

	/**
	 * Fills a new Shop's catalog with typical Quebec Frameless materials and returns a Construction
	 * Standard that uses them.
	 */
	ConstructionStandard seedDefaults(UUID shopId);
}
