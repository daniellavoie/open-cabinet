package dev.daniellavoie.opencabinet.member.module.web;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.daniellavoie.opencabinet.member.domain.model.Member;
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
}
