package dev.daniellavoie.opencabinet.client.module.adapter.database.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import dev.daniellavoie.opencabinet.client.domain.model.Client;
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
@Entity(name = "Client")
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientEntity {
	@Id
	UUID id;
	UUID shopId;
	String name;
	String email;
	String phone;
	String address;
	LocalDateTime creationTimestamp;
	LocalDateTime updateTimestamp;

	public Client to() {
		return Client.builder()
				.id(id)
				.name(name)
				.email(email)
				.phone(phone)
				.address(address)
				.build();
	}
}
