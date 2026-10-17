package dev.daniellavoie.opencabinet.member.module.web;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.daniellavoie.opencabinet.member.domain.model.Member;
import dev.daniellavoie.opencabinet.member.domain.model.RoleRequest;
import dev.daniellavoie.opencabinet.member.domain.port.api.MemberDomain;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@AllArgsConstructor
@RequestMapping("/api/member")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MemberController {
	MemberDomain domain;

	/**
	 * 404 tells the Angular app that this person has not joined a Shop yet.
	 */
	@GetMapping("/me")
	public ResponseEntity<Member> me(Principal principal) {
		return ResponseEntity.of(domain.findCurrent(principal));
	}

	@GetMapping
	public List<Member> findAll(Principal principal) {
		return domain.findByShop(principal);
	}

	@PutMapping("/{memberId}/role")
	public Member updateRole(@PathVariable UUID memberId, @RequestBody RoleRequest request, Principal principal) {
		return domain.updateRole(memberId, request.getRole(), principal);
	}

	@DeleteMapping("/{memberId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remove(@PathVariable UUID memberId, Principal principal) {
		domain.remove(memberId, principal);
	}
}
