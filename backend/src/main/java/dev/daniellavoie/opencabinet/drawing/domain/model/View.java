package dev.daniellavoie.opencabinet.drawing.domain.model;

import java.util.List;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * One drawing on a Sheet, such as a plan view, an Elevation or a Cabinet's side.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class View {
	String title;
	List<Shape> shapes;
}
