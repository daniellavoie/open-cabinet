package dev.daniellavoie.opencabinet.member.module.adapter.database.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import dev.daniellavoie.opencabinet.member.domain.model.Member;
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
@Entity(name = "Member")
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MemberEntity {
	@Id
	UUID id;
	UUID shopId;
	String subject;
	String name;
	String email;
	@Enumerated(EnumType.STRING)
	Role role;
	LocalDateTime creationTimestamp;

	public Member to() {
		return Member.builder()
				.id(id)
				.shopId(shopId)
				.subject(subject)
				.name(name)
				.email(email)
				.role(role)
				.build();
	}
}
