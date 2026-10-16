package dev.daniellavoie.opencabinet.member.module.adapter.database;

import java.time.LocalDateTime;
import java.util.Optional;

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
}
