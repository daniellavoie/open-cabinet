package dev.daniellavoie.opencabinet.invitation.module.adapter.database.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import dev.daniellavoie.opencabinet.invitation.domain.model.Invitation;
import dev.daniellavoie.opencabinet.member.domain.model.Role;
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
@Entity(name = "Invitation")
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvitationEntity {
	@Id
	UUID id;
	UUID shopId;
	String tokenHash;
	String email;
	@Enumerated(EnumType.STRING)
	Role role;
	UUID invitedBy;
	LocalDateTime expirationTimestamp;
	LocalDateTime acceptanceTimestamp;
	LocalDateTime creationTimestamp;

	public Invitation to() {
		return Invitation.builder()
				.id(id)
				.shopId(shopId)
				.email(email)
				.role(role)
				.invitedBy(invitedBy)
				.expirationTimestamp(expirationTimestamp)
				.acceptanceTimestamp(acceptanceTimestamp)
				.creationTimestamp(creationTimestamp)
				.build();
	}
}
