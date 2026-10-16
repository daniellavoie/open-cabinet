package dev.daniellavoie.opencabinet.shop.module.adapter.database;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import dev.daniellavoie.opencabinet.shop.domain.model.Shop;
import dev.daniellavoie.opencabinet.shop.domain.port.backend.ShopRepository;
import dev.daniellavoie.opencabinet.shop.module.adapter.database.entity.ShopEntity;
import jakarta.persistence.EntityManager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShopDatabaseRepository implements ShopRepository {
	ShopJpaRepository jpaRepository;
	EntityManager entityManager;

	@Override
	@Transactional
	public Shop create(Shop shop) {
		var now = LocalDateTime.now();
		return jpaRepository.save(ShopEntity.builder()
				.id(shop.getId())
				.name(shop.getName())
				.currency(shop.getCurrency())
				.units(shop.getUnits())
				.settings(shop.getSettings())
				.creationTimestamp(now)
				.updateTimestamp(now)
				.build())
				.to();
	}

	@Override
	@Transactional(readOnly = true)
	public Shop findById(UUID shopId) {
		return find(shopId).to();
	}

	@Override
	@Transactional
	public Shop update(Shop shop) {
		return jpaRepository.save(find(shop.getId()).toBuilder()
				.name(shop.getName())
				.currency(shop.getCurrency())
				.units(shop.getUnits())
				.settings(shop.getSettings())
				.updateTimestamp(LocalDateTime.now())
				.build())
				.to();
	}

	@Override
	@Transactional
	public int nextQuoteNumber(UUID shopId) {
		jpaRepository.incrementQuoteSequence(shopId);
		entityManager.clear();
		return find(shopId).getQuoteSequence();
	}

	private ShopEntity find(UUID shopId) {
		return jpaRepository.findById(shopId)
				.orElseThrow(() -> new NoSuchElementException("shop " + shopId + " not found"));
	}
}
