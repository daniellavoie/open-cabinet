package dev.daniellavoie.opencabinet.member.domain.port.api;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.daniellavoie.opencabinet.member.domain.model.Member;
import dev.daniellavoie.opencabinet.member.domain.model.Role;

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

	/**
	 * Adds the person behind the principal to a Shop, e.g. when they accept an Invitation.
	 */
	Member join(UUID shopId, Role role, String name, String email, Principal principal);

	List<Member> findByShop(Principal principal);

	Member updateRole(UUID memberId, Role role, Principal principal);

	void remove(UUID memberId, Principal principal);
}
