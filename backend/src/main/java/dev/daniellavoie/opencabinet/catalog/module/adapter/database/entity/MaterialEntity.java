package dev.daniellavoie.opencabinet.catalog.module.adapter.database.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.catalog.domain.model.MaterialKind;
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
@Entity(name = "Material")
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaterialEntity {
	@Id
	UUID id;
	UUID shopId;
	@Enumerated(EnumType.STRING)
	MaterialKind kind;
	String name;
	String sku;
	BigDecimal price;
	Double thicknessMm;
	Double lengthMm;
	Double widthMm;
	boolean grain;
	LocalDateTime creationTimestamp;
	LocalDateTime updateTimestamp;

	public Material to() {
		return Material.builder()
				.id(id)
				.kind(kind)
				.name(name)
				.sku(sku)
				.price(price)
				.thicknessMm(thicknessMm)
				.lengthMm(lengthMm)
				.widthMm(widthMm)
				.grain(grain)
				.build();
	}
}
