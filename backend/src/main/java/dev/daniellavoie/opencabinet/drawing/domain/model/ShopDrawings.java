package dev.daniellavoie.opencabinet.drawing.domain.model;

import java.time.LocalDate;
import java.util.List;

import dev.daniellavoie.opencabinet.shop.domain.model.Units;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * The drawings a Shop uses to build and install a Project: a plan view, an Elevation per Wall and
 * a Sheet per Cabinet, measured in the Shop's units.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class ShopDrawings {
	String shopName;
	String projectName;
	String clientName;
	LocalDate date;
	Units units;
	DrawingLanguage language;
	List<Sheet> sheets;
}
