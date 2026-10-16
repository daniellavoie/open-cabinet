package dev.daniellavoie.opencabinet.member.module.adapter.database;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.daniellavoie.opencabinet.member.module.adapter.database.entity.MemberEntity;

public interface MemberJpaRepository extends JpaRepository<MemberEntity, UUID> {
	Optional<MemberEntity> findBySubject(String subject);
}
