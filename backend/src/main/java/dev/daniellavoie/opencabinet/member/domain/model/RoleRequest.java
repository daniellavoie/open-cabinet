package dev.daniellavoie.opencabinet.member.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class RoleRequest {
	Role role;
}
