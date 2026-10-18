package dev.daniellavoie.opencabinet.cutlist.domain.model;

import dev.daniellavoie.opencabinet.project.domain.model.NestingLayout;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * What one layout would take for a material, to compare with the others.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class NestingAlternative {
	NestingLayout layout;
	int sheets;
	int cuts;
	/**
	 * Part area over the area of the sheets used.
	 */
	double utilization;
}
