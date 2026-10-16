package dev.daniellavoie.opencabinet.catalog.module.adapter.database;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.catalog.domain.port.backend.MaterialRepository;
import dev.daniellavoie.opencabinet.catalog.module.adapter.database.entity.MaterialEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MaterialDatabaseRepository implements MaterialRepository {
	MaterialJpaRepository jpaRepository;

	@Override
	@Transactional(readOnly = true)
	public List<Material> findByShop(UUID shopId) {
		return jpaRepository.findByShopIdOrderByKindAscNameAsc(shopId)
				.stream()
				.map(MaterialEntity::to)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Material findById(UUID materialId, UUID shopId) {
		return find(materialId, shopId).to();
	}

	@Override
	@Transactional
	public Material save(Material material, UUID shopId) {
		var now = LocalDateTime.now();
		var entity = jpaRepository.findByIdAndShopId(material.getId(), shopId)
				.orElseGet(() -> MaterialEntity.builder()
						.id(material.getId())
						.shopId(shopId)
						.creationTimestamp(now)
						.build());

		return jpaRepository.save(entity.toBuilder()
				.kind(material.getKind())
				.name(material.getName())
				.sku(material.getSku())
				.price(material.getPrice())
				.thicknessMm(material.getThicknessMm())
				.lengthMm(material.getLengthMm())
				.widthMm(material.getWidthMm())
				.grain(material.isGrain())
				.updateTimestamp(now)
				.build())
				.to();
	}

	@Override
	@Transactional
	public void delete(UUID materialId, UUID shopId) {
		jpaRepository.delete(find(materialId, shopId));
	}

	private MaterialEntity find(UUID materialId, UUID shopId) {
		return jpaRepository.findByIdAndShopId(materialId, shopId)
				.orElseThrow(() -> new NoSuchElementException("material " + materialId + " not found"));
	}
}
