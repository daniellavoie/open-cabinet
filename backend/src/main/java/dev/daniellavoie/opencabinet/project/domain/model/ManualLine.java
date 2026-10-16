package dev.daniellavoie.opencabinet.project.domain.model;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class ManualLine {
	String description;
	BigDecimal amount;
}
