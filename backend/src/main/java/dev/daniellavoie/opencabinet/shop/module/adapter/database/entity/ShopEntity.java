package dev.daniellavoie.opencabinet.shop.module.adapter.database.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import dev.daniellavoie.opencabinet.shop.domain.model.Shop;
import dev.daniellavoie.opencabinet.shop.domain.model.ShopSettings;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;
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
@Entity(name = "Shop")
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShopEntity {
	@Id
	UUID id;
	String name;
	String currency;
	@Enumerated(EnumType.STRING)
	Units units;
	@JdbcTypeCode(SqlTypes.JSON)
	ShopSettings settings;
	int quoteSequence;
	LocalDateTime creationTimestamp;
	LocalDateTime updateTimestamp;

	public Shop to() {
		return Shop.builder()
				.id(id)
				.name(name)
				.currency(currency)
				.units(units)
				.settings(settings)
				.build();
	}
}
