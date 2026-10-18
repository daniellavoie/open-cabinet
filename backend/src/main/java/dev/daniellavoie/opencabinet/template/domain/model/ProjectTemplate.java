package dev.daniellavoie.opencabinet.template.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A Cabinet Template designed for one Project (ADR 0006): its editable {@code draft} and its
 * published {@code versions}, oldest first, which never change (ADR 0005). Cabinets use it by its id
 * as their template code. {@code sourceCabinetId} is the Cabinet it was made from, if any.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class ProjectTemplate {
	UUID id;
	UUID projectId;
	String name;
	CabinetTemplate draft;
	List<CabinetTemplate> versions;
	String sourceCabinetId;
	LocalDateTime creationTimestamp;
	LocalDateTime updateTimestamp;
}
