package dev.daniellavoie.opencabinet.cutlist.domain;

import static dev.daniellavoie.opencabinet.cutlist.domain.CutListFixtures.CARCASS;
import static dev.daniellavoie.opencabinet.cutlist.domain.CutListFixtures.FRONT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.cutlist.domain.model.MaterialNesting;
import dev.daniellavoie.opencabinet.cutlist.domain.model.Part;
import dev.daniellavoie.opencabinet.cutlist.domain.model.PartRole;
import dev.daniellavoie.opencabinet.cutlist.domain.model.SheetPlacement;

class NesterTest {
	private static final double KERF = 4;
	private static final double TRIM = 10;

	@Test
	void partsStayInsideTheTrimmedSheetWithoutOverlapping() {
		var parts = new ArrayList<Part>();
		for (int index = 0; index < 20; index++) {
			parts.add(part(CARCASS, 774.7 - index * 7, 587.3 - index * 11));
		}

		var nesting = Nester.nest(CARCASS, parts, KERF, TRIM);

		assertThat(nesting.getOversizedParts()).isEmpty();
		assertThat(placed(nesting)).hasSize(20);
		for (var sheet : nesting.getSheets()) {
			var placements = sheet.getPlacements();
			for (var placement : placements) {
				assertThat(placement.getAlongMm()).isGreaterThanOrEqualTo(TRIM);
				assertThat(placement.getAcrossMm()).isGreaterThanOrEqualTo(TRIM);
				assertThat(placement.getAlongMm() + placement.getLengthMm()).isLessThanOrEqualTo(CARCASS.getLengthMm() - TRIM);
				assertThat(placement.getAcrossMm() + placement.getWidthMm()).isLessThanOrEqualTo(CARCASS.getWidthMm() - TRIM);
			}
			for (int second = 0; second < placements.size(); second++) {
				for (int first = 0; first < second; first++) {
					assertThat(separatedByKerf(placements.get(first), placements.get(second)))
							.as("placements %d and %d", first, second)
							.isTrue();
				}
			}
		}
	}

	@Test
	void smallPartsShareASheet() {
		var parts = Collections.nCopies(6, part(CARCASS, 400, 300));

		var nesting = Nester.nest(CARCASS, parts, KERF, TRIM);

		assertThat(nesting.getSheets()).hasSize(1);
		assertThat(nesting.getSheets().get(0).getUtilization()).isGreaterThan(0.2);
	}

	@Test
	void grainedPartsNeverTurn() {
		var tallNarrow = part(FRONT, 300, 1100);

		var nesting = Nester.nest(FRONT, List.of(tallNarrow), KERF, TRIM);

		var placement = placed(nesting).get(0);
		assertThat(placement.isRotated()).isFalse();
		assertThat(placement.getLengthMm()).isEqualTo(300);
	}

	@Test
	void ungrainedPartsTurnToFit() {
		var tooWideAcross = part(CARCASS, 300, 1500);

		var nesting = Nester.nest(CARCASS, List.of(tooWideAcross), KERF, TRIM);

		assertThat(nesting.getOversizedParts()).isEmpty();
		assertThat(placed(nesting).get(0).isRotated()).isTrue();
	}

	@Test
	void partsLargerThanTheSheetAreReported() {
		var nesting = Nester.nest(CARCASS, List.of(part(CARCASS, 3000, 300)), KERF, TRIM);

		assertThat(nesting.getSheets()).isEmpty();
		assertThat(nesting.getOversizedParts()).hasSize(1);
	}

	private static List<SheetPlacement> placed(MaterialNesting nesting) {
		return nesting.getSheets().stream().flatMap(sheet -> sheet.getPlacements().stream()).toList();
	}

	private static boolean separatedByKerf(SheetPlacement first, SheetPlacement second) {
		return first.getAlongMm() + first.getLengthMm() + KERF <= second.getAlongMm() + 1e-6
				|| second.getAlongMm() + second.getLengthMm() + KERF <= first.getAlongMm() + 1e-6
				|| first.getAcrossMm() + first.getWidthMm() + KERF <= second.getAcrossMm() + 1e-6
				|| second.getAcrossMm() + second.getWidthMm() + KERF <= first.getAcrossMm() + 1e-6;
	}

	private static Part part(Material material, double length, double width) {
		return Part.builder()
				.cabinetId("c1")
				.cabinetLabel("C1")
				.role(PartRole.SIDE)
				.materialId(material.getId())
				.lengthMm(length)
				.widthMm(width)
				.thicknessMm(material.getThicknessMm())
				.grain(material.isGrain())
				.build();
	}
}
