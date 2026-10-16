package dev.daniellavoie.opencabinet.quote.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class QuoteStatusRequest {
	QuoteStatus status;
}
