package dev.daniellavoie.opencabinet.member.domain;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import dev.daniellavoie.opencabinet.member.domain.model.Member;
import dev.daniellavoie.opencabinet.member.domain.model.Role;
import dev.daniellavoie.opencabinet.member.domain.port.api.MemberDomain;
import dev.daniellavoie.opencabinet.member.domain.port.backend.MemberRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultMemberDomain implements MemberDomain {
	MemberRepository repository;

	@Override
	public Optional<Member> findCurrent(Principal principal) {
		return repository.findBySubject(principal.getName());
	}

	@Override
	public Member current(Principal principal) {
		return findCurrent(principal)
				.orElseThrow(() -> new AccessDeniedException("you are not a member of a shop"));
	}

	@Override
	public Member currentOwner(Principal principal) {
		var member = current(principal);
		if (member.getRole() != Role.OWNER) {
			throw new AccessDeniedException("only a shop owner can do this");
		}
		return member;
	}

	@Override
	public Member createOwner(UUID shopId, String name, String email, Principal principal) {
		if (findCurrent(principal).isPresent()) {
			throw new IllegalStateException("you are already a member of a shop");
		}

		return repository.create(Member.builder()
				.id(UUID.randomUUID())
				.shopId(shopId)
				.subject(principal.getName())
				.name(Optional.ofNullable(name)
						.filter(value -> !value.isBlank())
						.orElseThrow(() -> new IllegalArgumentException("a member name is required")))
				.email(email)
				.role(Role.OWNER)
				.build());
	}
}
