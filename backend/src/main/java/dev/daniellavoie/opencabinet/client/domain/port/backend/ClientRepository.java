package dev.daniellavoie.opencabinet.client.domain.port.backend;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dev.daniellavoie.opencabinet.client.domain.model.Client;

public interface ClientRepository {
	Page<Client> findByShop(UUID shopId, Pageable pageable);

	Client findById(UUID clientId, UUID shopId);

	Client save(Client client, UUID shopId);
}
