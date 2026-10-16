package dev.daniellavoie.opencabinet.client.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class ClientRequest {
	String name;
	String email;
	String phone;
	String address;
}
