package dev.daniellavoie.opencabinet.template.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * What changed from the previous version; required from the second version on.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class PublishTemplateRequest {
	String changeNote;
}
