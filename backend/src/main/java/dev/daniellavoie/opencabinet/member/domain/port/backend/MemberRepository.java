package dev.daniellavoie.opencabinet.member.domain.port.backend;

import java.util.Optional;

import dev.daniellavoie.opencabinet.member.domain.model.Member;

public interface MemberRepository {
	Optional<Member> findBySubject(String subject);

	Member create(Member member);
}
