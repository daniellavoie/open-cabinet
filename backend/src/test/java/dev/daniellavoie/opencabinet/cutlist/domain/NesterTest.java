package dev.daniellavoie.opencabinet.cutlist.domain;

import static dev.daniellavoie.opencabinet.cutlist.domain.CutListFixtures.CARCASS;
import static dev.daniellavoie.opencabinet.cutlist.domain.CutListFixtures.FRONT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.cutlist.domain.model.MaterialNesting;
import dev.daniellavoie.opencabinet.cutlist.domain.model.NestingAlternative;
import dev.daniellavoie.opencabinet.cutlist.domain.model.Part;
import dev.daniellavoie.opencabinet.cutlist.domain.model.PartRole;
import dev.daniellavoie.opencabinet.cutlist.domain.model.SheetCut;
import dev.daniellavoie.opencabinet.cutlist.domain.model.SheetPlacement;
import dev.daniellavoie.opencabinet.project.domain.model.NestingLayout;

class NesterTest {
	private static final double KERF = 4;
	private static final double TRIM = 10;
	private static final Nester.Saw SAW = new Nester.Saw(KERF, TRIM, 0);

	@ParameterizedTest
	@EnumSource(NestingLayout.class)
	void partsStayInsideTheTrimmedSheetWithoutOverlapping(NestingLayout layout) {
		var parts = new ArrayList<Part>();
		for (int index = 0; index < 20; index++) {
			parts.add(part(CARCASS, 774.7 - index * 7, 587.3 - index * 11));
		}

		var nesting = Nester.nest(CARCASS, parts, new Nester.Saw(KERF, TRIM, 25.4), layout);

		assertThat(nesting.getLayout()).isEqualTo(layout);
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

		var nesting = Nester.nest(CARCASS, parts, SAW, null);

		assertThat(nesting.getSheets()).hasSize(1);
		assertThat(nesting.getSheets().get(0).getUtilization()).isGreaterThan(0.2);
	}

	@Test
	void grainedPartsNeverTurn() {
		var tallNarrow = part(FRONT, 300, 1100);

		var nesting = Nester.nest(FRONT, List.of(tallNarrow), SAW, null);

		var placement = placed(nesting).get(0);
		assertThat(placement.isRotated()).isFalse();
		assertThat(placement.getLengthMm()).isEqualTo(300);
	}

	@Test
	void ungrainedPartsTurnToFit() {
		var tooWideAcross = part(CARCASS, 300, 1500);

		var nesting = Nester.nest(CARCASS, List.of(tooWideAcross), SAW, null);

		assertThat(nesting.getOversizedParts()).isEmpty();
		assertThat(placed(nesting).get(0).isRotated()).isTrue();
	}

	@Test
	void partsLargerThanTheSheetAreReported() {
		var nesting = Nester.nest(CARCASS, List.of(part(CARCASS, 3000, 300)), SAW, null);

		assertThat(nesting.getSheets()).isEmpty();
		assertThat(nesting.getOversizedParts()).hasSize(1);
	}

	@Test
	void grainedPartsKeepTheirLengthAlongTheSheetWhenCrosscutFirst() {
		var nesting = Nester.nest(FRONT, List.of(part(FRONT, 700, 400), part(FRONT, 700, 400)), SAW,
				NestingLayout.CROSSCUT_FIRST);

		assertThat(placed(nesting)).allSatisfy(placement -> {
			assertThat(placement.isRotated()).isFalse();
			assertThat(placement.getLengthMm()).isEqualTo(700);
		});
		// Sections are crosscut along the sheet's length first, then ripped: both Parts in one section.
		assertThat(placed(nesting)).extracting(SheetPlacement::getAlongMm).containsOnly(TRIM);
	}

	@Test
	void shorterPartsStackInTheColumnOfATallerOne() {
		var nesting = Nester.nest(CARCASS, List.of(part(CARCASS, 1200, 590), part(CARCASS, 600, 290),
				part(CARCASS, 600, 290)), SAW, NestingLayout.RIP_FIRST);

		var placements = placed(nesting);
		assertThat(nesting.getSheets()).hasSize(1);
		assertThat(placements.get(1).getAlongMm()).isEqualTo(placements.get(2).getAlongMm());
		assertThat(placements.get(2).getAcrossMm()).isEqualTo(placements.get(1).getAcrossMm() + 290 + KERF);
	}

	@Test
	void cutsAreNumberedInCuttingOrder() {
		var nesting = Nester.nest(CARCASS, List.of(part(CARCASS, 1000, 500), part(CARCASS, 1000, 500)), SAW,
				NestingLayout.RIP_FIRST);

		// Rip the 500 mm strip off the sheet, then crosscut each Part off the strip.
		assertThat(nesting.getSheets().get(0).getCuts()).extracting(SheetCut::getNumber)
				.containsExactly("1", "1.1", "1.2");
		assertThat(nesting.getSheets().get(0).getCuts().get(0).getFromAcrossMm()).isEqualTo(TRIM + 500);
	}

	@Test
	void stripsAreNeverNarrowerThanTheMinimumOffcut() {
		var nesting = Nester.nest(CARCASS, List.of(part(CARCASS, 1000, 15)), new Nester.Saw(KERF, TRIM, 25.4),
				NestingLayout.RIP_FIRST);

		// The strip is ripped 25.4 mm wide, then the Part is ripped off it.
		assertThat(nesting.getSheets().get(0).getCuts())
				.anySatisfy(cut -> assertThat(cut.getFromAcrossMm()).isEqualTo(TRIM + 25.4))
				.anySatisfy(cut -> assertThat(cut.getFromAcrossMm()).isEqualTo(TRIM + 15));
	}

	@Test
	void everyLayoutIsComparedAndTheFewestSheetsKeptByDefault() {
		var parts = Collections.nCopies(6, part(CARCASS, 1100, 400));

		var automatic = Nester.nest(CARCASS, parts, SAW, null);

		assertThat(automatic.getAlternatives()).extracting(NestingAlternative::getLayout)
				.containsExactly(NestingLayout.RIP_FIRST, NestingLayout.CROSSCUT_FIRST);
		var fewest = automatic.getAlternatives().stream().mapToInt(NestingAlternative::getSheets).min().orElseThrow();
		assertThat(automatic.getSheets()).hasSize(fewest);
		assertThat(automatic.getAlternatives()).allSatisfy(alternative -> assertThat(alternative.getCuts()).isPositive());
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
