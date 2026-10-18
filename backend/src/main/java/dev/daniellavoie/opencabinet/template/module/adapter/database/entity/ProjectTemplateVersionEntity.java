package dev.daniellavoie.opencabinet.template.module.adapter.database.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "ProjectTemplateVersion")
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectTemplateVersionEntity {
	@EmbeddedId
	Key key;
	@JdbcTypeCode(SqlTypes.JSON)
	CabinetTemplate content;
	LocalDateTime publishTimestamp;

	@Data
	@Embeddable
	@NoArgsConstructor
	@AllArgsConstructor
	@FieldDefaults(level = AccessLevel.PRIVATE)
	public static class Key implements Serializable {
		private static final long serialVersionUID = 1L;

		UUID templateId;
		int version;
	}
}
