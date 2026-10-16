package dev.daniellavoie.opencabinet.project.domain.model;

import java.util.UUID;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class CreateProjectRequest {
	UUID clientId;
	String name;
}
