package dev.daniellavoie.opencabinet.catalog.domain.model;

import java.util.List;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * The outcome of a CSV import. An import is all or nothing: with any error, nothing is applied.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class CatalogImportResult {
	boolean applied;
	int created;
	int updated;
	List<ImportError> errors;

	@Value
	@Jacksonized
	@Builder(toBuilder = true)
	public static class ImportError {
		int line;
		String message;
	}
}
