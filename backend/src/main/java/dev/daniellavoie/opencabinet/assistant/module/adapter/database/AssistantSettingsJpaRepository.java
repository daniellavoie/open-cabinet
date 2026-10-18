package dev.daniellavoie.opencabinet.assistant.module.adapter.database;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.daniellavoie.opencabinet.assistant.module.adapter.database.entity.AssistantSettingsEntity;

public interface AssistantSettingsJpaRepository extends JpaRepository<AssistantSettingsEntity, UUID> {
}
