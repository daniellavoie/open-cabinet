package dev.daniellavoie.opencabinet.member.domain.port.backend;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.daniellavoie.opencabinet.member.domain.model.Member;

public interface MemberRepository {
	Optional<Member> findBySubject(String subject);

	List<Member> findByShop(UUID shopId);

	Member findById(UUID memberId, UUID shopId);

	Member create(Member member);

	Member update(Member member);

	void delete(UUID memberId, UUID shopId);
}
