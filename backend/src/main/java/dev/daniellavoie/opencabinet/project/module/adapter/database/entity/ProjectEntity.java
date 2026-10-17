package dev.daniellavoie.opencabinet.project.module.adapter.database.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.ManualLine;
import dev.daniellavoie.opencabinet.project.domain.model.Project;
import dev.daniellavoie.opencabinet.shop.domain.model.LabourMethod;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "Project")
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectEntity {
	@Id
	UUID id;
	UUID shopId;
	UUID clientId;
	String name;
	@JdbcTypeCode(SqlTypes.JSON)
	Design design;
	@Enumerated(EnumType.STRING)
	LabourMethod labourMethod;
	BigDecimal labourHours;
	@JdbcTypeCode(SqlTypes.JSON)
	List<ManualLine> manualLines;
	UUID lockedBy;
	LocalDateTime lockExpirationTimestamp;
	LocalDateTime creationTimestamp;
	LocalDateTime updateTimestamp;

	public Project to() {
		return Project.builder()
				.id(id)
				.clientId(clientId)
				.name(name)
				.design(design)
				.labourMethod(labourMethod)
				.labourHours(labourHours)
				.manualLines(manualLines)
				.creationTimestamp(creationTimestamp)
				.updateTimestamp(updateTimestamp)
				.build();
	}
}
