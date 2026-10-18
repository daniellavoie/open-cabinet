package dev.daniellavoie.opencabinet.cutlist.domain.model;

import dev.daniellavoie.opencabinet.project.domain.model.PartRole;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Where one Part sits on a sheet: {@code alongMm} is measured along the sheet's length (and its
 * grain), {@code acrossMm} across its width, both from the sheet's corner.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class SheetPlacement {
	String cabinetLabel;
	PartRole role;
	double alongMm;
	double acrossMm;
	double lengthMm;
	double widthMm;
	boolean rotated;
}
