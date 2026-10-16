package dev.daniellavoie.opencabinet.catalog.module.web;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.catalog.domain.model.MaterialRequest;
import dev.daniellavoie.opencabinet.catalog.domain.port.api.CatalogDomain;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@AllArgsConstructor
@RequestMapping("/api/material")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CatalogController {
	CatalogDomain domain;

	@GetMapping
	public List<Material> findAll(Principal principal) {
		return domain.findAll(principal);
	}

	@PostMapping
	public Material create(@RequestBody MaterialRequest request, Principal principal) {
		return domain.create(request, principal);
	}

	@PutMapping("/{materialId}")
	public Material update(@PathVariable UUID materialId, @RequestBody MaterialRequest request,
			Principal principal) {
		return domain.update(materialId, request, principal);
	}

	@DeleteMapping("/{materialId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID materialId, Principal principal) {
		domain.delete(materialId, principal);
	}
}
