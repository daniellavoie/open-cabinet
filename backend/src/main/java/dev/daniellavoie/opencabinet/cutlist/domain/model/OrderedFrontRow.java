package dev.daniellavoie.opencabinet.cutlist.domain.model;

import java.util.List;
import java.util.UUID;

import dev.daniellavoie.opencabinet.project.domain.model.PartRole;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Identical Fronts to order from a supplier, in one style, with the Cabinets they belong to.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class OrderedFrontRow {
	PartRole role;
	/**
	 * The template's name for an OTHER Part.
	 */
	String name;
	UUID materialId;
	String materialName;
	double heightMm;
	double widthMm;
	int quantity;
	List<String> cabinetLabels;
}
