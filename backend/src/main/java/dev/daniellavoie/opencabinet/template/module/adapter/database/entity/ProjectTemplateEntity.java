package dev.daniellavoie.opencabinet.template.module.adapter.database.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import jakarta.persistence.Entity;
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
@Entity(name = "ProjectTemplate")
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectTemplateEntity {
	@Id
	UUID id;
	UUID shopId;
	UUID projectId;
	String name;
	@JdbcTypeCode(SqlTypes.JSON)
	CabinetTemplate draft;
	String sourceCabinetId;
	LocalDateTime creationTimestamp;
	LocalDateTime updateTimestamp;
}
