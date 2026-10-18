package dev.daniellavoie.opencabinet.assistant.module.adapter.database.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "AssistantUsage")
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AssistantUsageEntity {
	@Id
	UUID id;
	UUID shopId;
	UUID memberId;
	String purpose;
	String model;
	long inputTokens;
	long outputTokens;
	BigDecimal costUsd;
	LocalDateTime creationTimestamp;
}
