package dev.daniellavoie.opencabinet.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Keycloak is identity only (docs/adr/0003-keycloak-for-identity-only.md): these settings are used
 * to validate tokens and to let the Angular app sign in, never to read Shop membership or roles.
 */
@Data
@Configuration
@ConfigurationProperties("open-cabinet.keycloak")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KeycloakConfiguration {
	/**
	 * Realm issuer, e.g. https://auth.example.ca/realms/open-cabinet.
	 */
	String issuerUri;

	/**
	 * Public client used by the Angular app.
	 */
	String clientId;
}
