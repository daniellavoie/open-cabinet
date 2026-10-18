package dev.daniellavoie.opencabinet.drawing.domain.model;

import java.util.List;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A Cabinet's Parts, printed under its drawings.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class PartsTable {
	List<String> headers;
	List<List<String>> rows;
}
