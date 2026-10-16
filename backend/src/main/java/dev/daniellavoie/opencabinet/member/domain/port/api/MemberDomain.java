package dev.daniellavoie.opencabinet.member.domain.port.api;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

import dev.daniellavoie.opencabinet.member.domain.model.Member;

public interface MemberDomain {
	Optional<Member> findCurrent(Principal principal);

	/**
	 * The Member behind the principal. Rejected when the person has not joined a Shop.
	 */
	Member current(Principal principal);

	/**
	 * The Member behind the principal, rejected unless they are an Owner of their Shop.
	 */
	Member currentOwner(Principal principal);

	Member createOwner(UUID shopId, String name, String email, Principal principal);
}
