package dev.daniellavoie.opencabinet.client.module.web;

import java.security.Principal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.daniellavoie.opencabinet.client.domain.model.Client;
import dev.daniellavoie.opencabinet.client.domain.model.ClientRequest;
import dev.daniellavoie.opencabinet.client.domain.port.api.ClientDomain;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@AllArgsConstructor
@RequestMapping("/api/client")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClientController {
	ClientDomain domain;

	@GetMapping
	public Page<Client> findAll(Principal principal,
			@PageableDefault(size = 50, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
		return domain.findByPrincipal(principal, pageable);
	}

	@GetMapping("/{clientId}")
	public Client findById(@PathVariable UUID clientId, Principal principal) {
		return domain.findById(clientId, principal);
	}

	@PostMapping
	public Client create(@RequestBody ClientRequest request, Principal principal) {
		return domain.create(request, principal);
	}

	@PutMapping("/{clientId}")
	public Client update(@PathVariable UUID clientId, @RequestBody ClientRequest request, Principal principal) {
		return domain.update(clientId, request, principal);
	}
}
