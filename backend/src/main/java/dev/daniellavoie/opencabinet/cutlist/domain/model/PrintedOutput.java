package dev.daniellavoie.opencabinet.cutlist.domain.model;

import java.time.LocalDate;
import java.util.List;

import dev.daniellavoie.opencabinet.drawing.domain.model.DrawingLanguage;
import dev.daniellavoie.opencabinet.drawing.domain.model.View;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * One of a Project's outputs, ready to print: its Cut List, its Nesting (one View per sheet) or its
 * Bill of Materials, with what goes in the title block.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class PrintedOutput {
	OutputDocument document;
	String shopName;
	String projectName;
	String clientName;
	LocalDate date;
	Units units;
	String currency;
	DrawingLanguage language;
	ProjectOutput output;
	/**
	 * For Nesting: each sheet drawn with its Parts, titled with its material and number.
	 */
	List<View> sheets;
}
