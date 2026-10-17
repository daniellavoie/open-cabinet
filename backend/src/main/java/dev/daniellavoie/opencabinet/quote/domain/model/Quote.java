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
	/**
	 * 0 for the original Quote, then 1, 2… for each Quote Revision.
	 */
	int revision;
	QuoteStatus status;
	QuoteContent content;
	LocalDateTime creationTimestamp;
	LocalDateTime sentTimestamp;

	/**
	 * The number printed on the Quote, e.g. S-0042, or S-0042-R1 for its first revision.
	 */
	public String getDisplayNumber() {
		return revision == 0 ? String.format("S-%04d", number) : String.format("S-%04d-R%d", number, revision);
	}
}
