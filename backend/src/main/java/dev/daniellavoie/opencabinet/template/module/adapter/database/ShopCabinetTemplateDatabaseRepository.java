package dev.daniellavoie.opencabinet.template.module.adapter.database;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import dev.daniellavoie.opencabinet.template.domain.model.ShopCabinetTemplate;
import dev.daniellavoie.opencabinet.template.domain.port.backend.ShopCabinetTemplateRepository;
import dev.daniellavoie.opencabinet.template.module.adapter.database.entity.ShopCabinetTemplateEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShopCabinetTemplateDatabaseRepository implements ShopCabinetTemplateRepository {
	ShopCabinetTemplateJpaRepository jpaRepository;

	@Override
	@Transactional(readOnly = true)
	public List<ShopCabinetTemplate> findByShop(UUID shopId) {
		return jpaRepository.findByShopIdOrderByName(shopId).stream().map(ShopCabinetTemplateEntity::to).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<UUID> findShopsWithPresets() {
		return jpaRepository.findShopIds();
	}

	@Override
	@Transactional
	public ShopCabinetTemplate save(ShopCabinetTemplate template, UUID shopId) {
		return jpaRepository.save(ShopCabinetTemplateEntity.builder()
				.id(template.getId())
				.shopId(shopId)
				.name(template.getName())
				.baseCode(template.getBaseCode())
				.preset(template.getPreset())
				.creationTimestamp(LocalDateTime.now())
				.build())
				.to();
	}

	@Override
	@Transactional
	public void delete(UUID templateId, UUID shopId) {
		jpaRepository.delete(jpaRepository.findByIdAndShopId(templateId, shopId)
				.orElseThrow(() -> new NoSuchElementException("template " + templateId + " not found")));
	}
}
