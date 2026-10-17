package dev.daniellavoie.opencabinet.member.module.adapter.database;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import dev.daniellavoie.opencabinet.member.domain.model.Member;
import dev.daniellavoie.opencabinet.member.domain.port.backend.MemberRepository;
import dev.daniellavoie.opencabinet.member.module.adapter.database.entity.MemberEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MemberDatabaseRepository implements MemberRepository {
	MemberJpaRepository jpaRepository;

	@Override
	@Transactional(readOnly = true)
	public Optional<Member> findBySubject(String subject) {
		return jpaRepository.findBySubject(subject).map(MemberEntity::to);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Member> findByShop(UUID shopId) {
		return jpaRepository.findByShopIdOrderByNameAsc(shopId).stream().map(MemberEntity::to).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Member findById(UUID memberId, UUID shopId) {
		return find(memberId, shopId).to();
	}

	@Override
	@Transactional
	public Member create(Member member) {
		log.info("Creating member {} with: { shopId = {}, role = {} }", member.getId(), member.getShopId(),
				member.getRole());

		return jpaRepository.save(MemberEntity.builder()
				.id(member.getId())
				.shopId(member.getShopId())
				.subject(member.getSubject())
				.name(member.getName())
				.email(member.getEmail())
				.role(member.getRole())
				.creationTimestamp(LocalDateTime.now())
				.build())
				.to();
	}

	@Override
	@Transactional
	public Member update(Member member) {
		log.info("Updating member {} with: { shopId = {}, role = {} }", member.getId(), member.getShopId(),
				member.getRole());

		return jpaRepository.save(find(member.getId(), member.getShopId()).toBuilder()
				.name(member.getName())
				.email(member.getEmail())
				.role(member.getRole())
				.build())
				.to();
	}

	@Override
	@Transactional
	public void delete(UUID memberId, UUID shopId) {
		log.info("Removing member {} from shop {}", memberId, shopId);
		jpaRepository.delete(find(memberId, shopId));
	}

	private MemberEntity find(UUID memberId, UUID shopId) {
		return jpaRepository.findByIdAndShopId(memberId, shopId)
				.orElseThrow(() -> new NoSuchElementException("member " + memberId + " not found"));
	}
}
