package dev.daniellavoie.opencabinet.quote.module.adapter.database.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import dev.daniellavoie.opencabinet.quote.domain.model.Quote;
import dev.daniellavoie.opencabinet.quote.domain.model.QuoteContent;
import dev.daniellavoie.opencabinet.quote.domain.model.QuoteStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Entity(name = "Quote")
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuoteEntity {
	@Id
	UUID id;
	UUID shopId;
	UUID projectId;
	int number;
	int revision;
	@Enumerated(EnumType.STRING)
	QuoteStatus status;
	@JdbcTypeCode(SqlTypes.JSON)
	QuoteContent content;
	LocalDateTime creationTimestamp;
	LocalDateTime sentTimestamp;

	public Quote to() {
		return Quote.builder()
				.id(id)
				.projectId(projectId)
				.number(number)
				.revision(revision)
				.status(status)
				.content(content)
				.creationTimestamp(creationTimestamp)
				.sentTimestamp(sentTimestamp)
				.build();
	}
}
