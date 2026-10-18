package dev.daniellavoie.opencabinet.drawing.domain.model;

import java.util.List;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * One printed page of the Shop Drawings. Its Views share one scale; {@code notes} are printed
 * under them, then the Parts table, if any.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class Sheet {
	String title;
	List<View> views;
	List<String> notes;
	PartsTable parts;
}
