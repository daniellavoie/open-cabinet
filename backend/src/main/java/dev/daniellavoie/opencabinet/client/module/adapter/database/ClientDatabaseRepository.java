package dev.daniellavoie.opencabinet.client.module.adapter.database;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import dev.daniellavoie.opencabinet.client.domain.model.Client;
import dev.daniellavoie.opencabinet.client.domain.port.backend.ClientRepository;
import dev.daniellavoie.opencabinet.client.module.adapter.database.entity.ClientEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClientDatabaseRepository implements ClientRepository {
	ClientJpaRepository jpaRepository;

	@Override
	@Transactional(readOnly = true)
	public Page<Client> findByShop(UUID shopId, Pageable pageable) {
		return jpaRepository.findByShopId(shopId, pageable).map(ClientEntity::to);
	}

	@Override
	@Transactional(readOnly = true)
	public Client findById(UUID clientId, UUID shopId) {
		return jpaRepository.findByIdAndShopId(clientId, shopId)
				.map(ClientEntity::to)
				.orElseThrow(() -> new NoSuchElementException("client " + clientId + " not found"));
	}

	@Override
	@Transactional
	public Client save(Client client, UUID shopId) {
		var now = LocalDateTime.now();
		var entity = jpaRepository.findByIdAndShopId(client.getId(), shopId)
				.orElseGet(() -> ClientEntity.builder()
						.id(client.getId())
						.shopId(shopId)
						.creationTimestamp(now)
						.build());

		return jpaRepository.save(entity.toBuilder()
				.name(client.getName())
				.email(client.getEmail())
				.phone(client.getPhone())
				.address(client.getAddress())
				.updateTimestamp(now)
				.build())
				.to();
	}
}
