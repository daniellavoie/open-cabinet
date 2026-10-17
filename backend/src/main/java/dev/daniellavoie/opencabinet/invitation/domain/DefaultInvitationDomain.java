package dev.daniellavoie.opencabinet.invitation.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dev.daniellavoie.opencabinet.invitation.domain.model.AcceptInvitationRequest;
import dev.daniellavoie.opencabinet.invitation.domain.model.CreatedInvitation;
import dev.daniellavoie.opencabinet.invitation.domain.model.Invitation;
import dev.daniellavoie.opencabinet.invitation.domain.model.InvitationPreview;
import dev.daniellavoie.opencabinet.invitation.domain.model.InvitationRequest;
import dev.daniellavoie.opencabinet.invitation.domain.port.api.InvitationDomain;
import dev.daniellavoie.opencabinet.invitation.domain.port.backend.InvitationRepository;
import dev.daniellavoie.opencabinet.member.domain.model.Member;
import dev.daniellavoie.opencabinet.member.domain.port.api.MemberDomain;
import dev.daniellavoie.opencabinet.member.domain.port.backend.MemberRepository;
import dev.daniellavoie.opencabinet.shop.domain.port.backend.ShopRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultInvitationDomain implements InvitationDomain {
	static final Duration VALIDITY = Duration.ofDays(14);
	private static final SecureRandom RANDOM = new SecureRandom();

	InvitationRepository repository;
	MemberDomain memberDomain;
	MemberRepository memberRepository;
	ShopRepository shopRepository;

	@Override
	public CreatedInvitation create(InvitationRequest request, Principal principal) {
		var owner = memberDomain.currentOwner(principal);
		var role = Optional.ofNullable(request.getRole())
				.orElseThrow(() -> new IllegalArgumentException("a role is required"));
		var token = newToken();
		var now = LocalDateTime.now();

		var invitation = repository.create(Invitation.builder()
				.id(UUID.randomUUID())
				.shopId(owner.getShopId())
				.email(Optional.ofNullable(request.getEmail()).filter(email -> !email.isBlank()).orElse(null))
				.role(role)
				.invitedBy(owner.getId())
				.expirationTimestamp(now.plus(VALIDITY))
				.creationTimestamp(now)
				.build(), hash(token));

		return CreatedInvitation.builder().invitation(invitation).token(token).build();
	}

	@Override
	public List<Invitation> findPending(Principal principal) {
		return repository.findPending(memberDomain.currentOwner(principal).getShopId());
	}

	@Override
	public void revoke(UUID invitationId, Principal principal) {
		repository.delete(invitationId, memberDomain.currentOwner(principal).getShopId());
	}

	@Override
	public InvitationPreview preview(String token) {
		var invitation = usable(token);

		return InvitationPreview.builder()
				.shopName(shopRepository.findById(invitation.getShopId()).getName())
				.role(invitation.getRole())
				.invitedByName(Optional.ofNullable(invitation.getInvitedBy())
						.map(memberId -> memberRepository.findById(memberId, invitation.getShopId()).getName())
						.orElse(null))
				.build();
	}

	@Override
	public Member accept(String token, AcceptInvitationRequest request, Principal principal) {
		var invitation = usable(token);
		if (memberDomain.findCurrent(principal).isPresent()) {
			throw new IllegalStateException("you are already a member of a shop");
		}

		repository.markAccepted(invitation.getId());
		return memberDomain.join(invitation.getShopId(), invitation.getRole(), request.getMemberName(),
				Optional.ofNullable(request.getMemberEmail()).orElse(invitation.getEmail()), principal);
	}

	/**
	 * The Invitation behind a token, as long as it is neither accepted nor expired. Unknown, used
	 * and expired tokens look the same to the caller.
	 */
	private Invitation usable(String token) {
		return Optional.ofNullable(token)
				.flatMap(value -> repository.findByTokenHash(hash(value)))
				.filter(invitation -> invitation.getAcceptanceTimestamp() == null)
				.filter(invitation -> invitation.getExpirationTimestamp().isAfter(LocalDateTime.now()))
				.orElseThrow(() -> new NoSuchElementException("this invitation is invalid or has expired"));
	}

	static String newToken() {
		var bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	static String hash(String token) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
