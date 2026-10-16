package dev.daniellavoie.opencabinet.member.domain.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class Member {
	UUID id;
	UUID shopId;
	/**
	 * Keycloak subject of the person behind this Member.
	 */
	@JsonIgnore
	String subject;
	String name;
	String email;
	Role role;
}
