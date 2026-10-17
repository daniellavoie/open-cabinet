package dev.daniellavoie.opencabinet.template.module.adapter.database.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.template.domain.model.ShopCabinetTemplate;
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
@Entity(name = "ShopCabinetTemplate")
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShopCabinetTemplateEntity {
	@Id
	UUID id;
	UUID shopId;
	String name;
	String baseCode;
	@JdbcTypeCode(SqlTypes.JSON)
	Cabinet preset;
	LocalDateTime creationTimestamp;

	public ShopCabinetTemplate to() {
		return ShopCabinetTemplate.builder()
				.id(id)
				.name(name)
				.baseCode(baseCode)
				.preset(preset)
				.build();
	}
}
