package dev.daniellavoie.opencabinet.client.domain.model;

import java.util.UUID;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class Client {
	UUID id;
	String name;
	String email;
	String phone;
	String address;
}
