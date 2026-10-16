package dev.daniellavoie.opencabinet.security.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class SecurityConfigResponse {
	String issuerUri;
	String clientId;
}
