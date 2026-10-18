package dev.daniellavoie.opencabinet.template.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A Cabinet Template a Member designed (ADR 0006): its editable {@code draft} and its published
 * {@code versions}, oldest first, which never change (ADR 0005). Cabinets use it by its id as their
 * template code. It belongs to its Project ({@code projectId}) until {@code shared} with the Shop
 * library, where every Project can use it; templates created in the library have no Project.
 * {@code archived} ones are hidden when adding Cabinets. {@code sourceCabinetId} is the Cabinet it
 * was made from, if any.
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
	boolean shared;
	boolean archived;
	LocalDateTime creationTimestamp;
	LocalDateTime updateTimestamp;
}
