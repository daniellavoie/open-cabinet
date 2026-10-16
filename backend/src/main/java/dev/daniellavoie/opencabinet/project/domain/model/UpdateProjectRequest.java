package dev.daniellavoie.opencabinet.project.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import dev.daniellavoie.opencabinet.shop.domain.model.LabourMethod;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class UpdateProjectRequest {
	UUID clientId;
	String name;
	Design design;
	LabourMethod labourMethod;
	BigDecimal labourHours;
	List<ManualLine> manualLines;
}
