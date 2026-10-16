package dev.daniellavoie.opencabinet.client.domain.port.api;

import java.security.Principal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dev.daniellavoie.opencabinet.client.domain.model.Client;
import dev.daniellavoie.opencabinet.client.domain.model.ClientRequest;

public interface ClientDomain {
	Page<Client> findByPrincipal(Principal principal, Pageable pageable);

	Client findById(UUID clientId, Principal principal);

	Client findById(UUID clientId, UUID shopId);

	Client create(ClientRequest request, Principal principal);

	Client update(UUID clientId, ClientRequest request, Principal principal);
}
