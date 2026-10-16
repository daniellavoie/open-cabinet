package dev.daniellavoie.opencabinet.security.module.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.daniellavoie.opencabinet.security.config.KeycloakConfiguration;
import dev.daniellavoie.opencabinet.security.domain.model.SecurityConfigResponse;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * Tells the Angular app which Keycloak realm and client to sign in with, so one image works in
 * every environment.
 */
@RestController
@RequestMapping("/api/security/config")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfigController {
	KeycloakConfiguration keycloakConfiguration;

	public SecurityConfigController(KeycloakConfiguration keycloakConfiguration) {
		if (keycloakConfiguration.getClientId() == null || keycloakConfiguration.getClientId().isEmpty()) {
			throw new IllegalArgumentException("property 'open-cabinet.keycloak.client-id' is required");
		}
		this.keycloakConfiguration = keycloakConfiguration;
	}

	@GetMapping
	public SecurityConfigResponse config() {
		return SecurityConfigResponse.builder()
				.issuerUri(keycloakConfiguration.getIssuerUri())
				.clientId(keycloakConfiguration.getClientId())
				.build();
	}
}
