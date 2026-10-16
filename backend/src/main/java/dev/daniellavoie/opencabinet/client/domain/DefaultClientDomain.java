package dev.daniellavoie.opencabinet.client.domain;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import dev.daniellavoie.opencabinet.client.domain.model.Client;
import dev.daniellavoie.opencabinet.client.domain.model.ClientRequest;
import dev.daniellavoie.opencabinet.client.domain.port.api.ClientDomain;
import dev.daniellavoie.opencabinet.client.domain.port.backend.ClientRepository;
import dev.daniellavoie.opencabinet.member.domain.port.api.MemberDomain;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultClientDomain implements ClientDomain {
	ClientRepository repository;
	MemberDomain memberDomain;

	@Override
	public Page<Client> findByPrincipal(Principal principal, Pageable pageable) {
		return repository.findByShop(memberDomain.current(principal).getShopId(), pageable);
	}

	@Override
	public Client findById(UUID clientId, Principal principal) {
		return repository.findById(clientId, memberDomain.current(principal).getShopId());
	}

	@Override
	public Client findById(UUID clientId, UUID shopId) {
		return repository.findById(clientId, shopId);
	}

	@Override
	public Client create(ClientRequest request, Principal principal) {
		return repository.save(toClient(UUID.randomUUID(), request), memberDomain.current(principal).getShopId());
	}

	@Override
	public Client update(UUID clientId, ClientRequest request, Principal principal) {
		var shopId = memberDomain.current(principal).getShopId();
		repository.findById(clientId, shopId);

		return repository.save(toClient(clientId, request), shopId);
	}

	private static Client toClient(UUID clientId, ClientRequest request) {
		return Client.builder()
				.id(clientId)
				.name(Optional.ofNullable(request.getName())
						.filter(value -> !value.isBlank())
						.orElseThrow(() -> new IllegalArgumentException("a client name is required")))
				.email(request.getEmail())
				.phone(request.getPhone())
				.address(request.getAddress())
				.build();
	}
}
