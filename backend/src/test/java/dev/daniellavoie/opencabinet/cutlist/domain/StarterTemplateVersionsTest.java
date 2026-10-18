package dev.daniellavoie.opencabinet.cutlist.domain;

import static dev.daniellavoie.opencabinet.cutlist.domain.CutListFixtures.CATALOG;
import static dev.daniellavoie.opencabinet.cutlist.domain.CutListFixtures.STANDARD;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.cutlist.domain.model.Part;
import dev.daniellavoie.opencabinet.project.domain.CabinetAssemblies;
import dev.daniellavoie.opencabinet.project.domain.TemplateRules;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.FrontSource;

/**
 * Version 2 of each starter template is version 1 redrawn as an assembly (ADR 0006): at every
 * sampled size and choice, both cut the same Parts and need the same hardware, so moving a Cabinet to
 * version 2 changes nothing it is built from. Lengths may differ by the Cut List's 0.1 mm rounding
 * step when a size lands on a half: a mirrored blind corner computes 507.549999… where version 1
 * computes 507.55.
 */
class StarterTemplateVersionsTest {
	private static final Map<UUID, Material> MATERIALS = CATALOG.stream()
			.collect(Collectors.toMap(Material::getId, Function.identity()));

	static Stream<String> codes() {
		return SampleKitchen.templates().stream().map(CabinetTemplate::getCode).distinct();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("codes")
	void version2CutsTheSamePartsAsVersion1(String code) {
		var first = template(code, 1);
		var second = template(code, 2);
		assertThat(second.getAssembly()).isNotNull();

		var compared = 0;
		for (var cabinet : cabinets(first)) {
			var before = PartGenerator.generate(cabinet.toBuilder().templateVersion(1).build(), first, "C1", STANDARD,
					MATERIALS);
			var after = PartGenerator.generate(cabinet.toBuilder().templateVersion(2).build(), second, "C1", STANDARD,
					MATERIALS);
			var expected = sorted(before.parts());
			var actual = sorted(after.parts());
			assertThat(actual).as("%s parts for %s", code, cabinet).hasSameSizeAs(expected);
			for (int index = 0; index < expected.size(); index++) {
				assertThat(same(actual.get(index), expected.get(index)))
						.as("%s part %s for %s, version 1 cuts %s", code, actual.get(index), cabinet, expected.get(index))
						.isTrue();
			}
			assertThat(after.hardware()).as("%s hardware for %s", code, cabinet)
					.containsExactlyInAnyOrderEntriesOf(before.hardware());
			compared++;
		}
		assertThat(compared).isGreaterThan(100);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("codes")
	void version2PassesTheTemplateRules(String code) {
		var shop = new TemplateRules.Shop(CabinetAssemblies.standardValues(STANDARD),
				material -> MATERIALS.get(CabinetAssemblies.materialId(material, STANDARD)).getThicknessMm(),
				material -> {
					var sheet = MATERIALS.get(CabinetAssemblies.materialId(material, STANDARD));
					return sheet.getLengthMm() == null ? null
							: new TemplateRules.Sheet(sheet.getLengthMm(), sheet.getWidthMm(), sheet.isGrain());
				},
				STANDARD.getSheetTrimMm());

		assertThat(TemplateRules.check(template(code, 2), shop)).isEmpty();
	}

	/**
	 * Every size at the template's minimum, default and maximum, with no, default and most shelves,
	 * a divider when allowed, Fronts made and ordered, finished ends or not, and mirrored or not.
	 */
	private static List<Cabinet> cabinets(CabinetTemplate template) {
		var cabinets = new ArrayList<Cabinet>();
		for (var width : List.of(template.getMinWidthMm(), template.getDefaultWidthMm(), template.getMaxWidthMm())) {
			for (var height : List.of(template.getMinHeightMm(), template.getDefaultHeightMm(), template.getMaxHeightMm())) {
				for (var depth : List.of(template.getMinDepthMm(), template.getDefaultDepthMm(), template.getMaxDepthMm())) {
					for (var shelves : List.of(0, template.getDefaultShelfCount(), template.getMaxShelfCount())) {
						for (var divided : template.allowsDividers() ? List.of(false, true) : List.of(false)) {
							for (var ordered : List.of(false, true)) {
								for (var mirrored : List.of(false, true)) {
									cabinets.add(Cabinet.builder().id("c1").templateCode(template.getCode()).wallId("w1")
											.widthMm(width).heightMm(height).depthMm(depth)
											.elevationMm(template.getDefaultElevationMm()).shelfCount(shelves)
											.dividerPositionsMm(divided ? List.of(width / 2) : null)
											.frontSource(ordered ? FrontSource.ORDERED : null)
											.finishedLeft(ordered).finishedRight(ordered).mirrored(mirrored).build());
								}
							}
						}
					}
				}
			}
		}
		return cabinets;
	}

	private static CabinetTemplate template(String code, int version) {
		return SampleKitchen.templates().stream()
				.filter(template -> template.getCode().equals(code) && template.getVersion() == version)
				.findFirst()
				.orElseThrow();
	}

	private static List<Part> sorted(List<Part> parts) {
		return parts.stream()
				.sorted(Comparator.comparing((Part part) -> part.getRole().name())
						.thenComparing(part -> String.valueOf(part.isOrdered()))
						.thenComparingDouble(Part::getLengthMm)
						.thenComparingDouble(Part::getWidthMm))
				.toList();
	}

	/**
	 * The same Part, lengths within the Cut List's rounding step.
	 */
	private static boolean same(Part actual, Part expected) {
		return actual.toBuilder().lengthMm(0).widthMm(0).build().equals(expected.toBuilder().lengthMm(0).widthMm(0).build())
				&& Math.abs(actual.getLengthMm() - expected.getLengthMm()) <= ROUNDING_STEP_MM
				&& Math.abs(actual.getWidthMm() - expected.getWidthMm()) <= ROUNDING_STEP_MM;
	}

	private static final double ROUNDING_STEP_MM = 0.1 + 1e-9;
}
