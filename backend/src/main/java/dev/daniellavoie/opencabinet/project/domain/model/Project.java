package dev.daniellavoie.opencabinet.project.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import dev.daniellavoie.opencabinet.shop.domain.model.LabourMethod;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class Project {
	UUID id;
	UUID clientId;
	String name;
	Design design;
	/**
	 * Labour Method for this Project; null uses the Shop's default.
	 */
	LabourMethod labourMethod;
	/**
	 * Estimated hours, used by the HOURLY Labour Method.
	 */
	BigDecimal labourHours;
	List<ManualLine> manualLines;
	LocalDateTime creationTimestamp;
	LocalDateTime updateTimestamp;
}
