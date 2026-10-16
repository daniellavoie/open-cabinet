package dev.daniellavoie.opencabinet.quote.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class Quote {
	UUID id;
	UUID projectId;
	int number;
	QuoteStatus status;
	QuoteContent content;
	LocalDateTime creationTimestamp;
	LocalDateTime sentTimestamp;

	/**
	 * The number printed on the Quote, e.g. S-0042.
	 */
	public String getDisplayNumber() {
		return String.format("S-%04d", number);
	}
}
