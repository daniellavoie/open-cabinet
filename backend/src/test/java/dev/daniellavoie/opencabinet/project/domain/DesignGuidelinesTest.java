package dev.daniellavoie.opencabinet.project.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import dev.daniellavoie.opencabinet.common.config.JsonConfiguration;
import dev.daniellavoie.opencabinet.project.domain.model.Appliance;
import dev.daniellavoie.opencabinet.project.domain.model.ApplianceKind;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.Filler;
import dev.daniellavoie.opencabinet.project.domain.model.Opening;
import dev.daniellavoie.opencabinet.project.domain.model.OpeningKind;
import dev.daniellavoie.opencabinet.project.domain.model.Remark;
import dev.daniellavoie.opencabinet.project.domain.model.Wall;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

/**
 * A well-laid L-shaped kitchen gets no remark; each test breaks one guideline in it.
 */
class DesignGuidelinesTest {
	private static final double IN = 25.4;
	private static final List<CabinetTemplate> TEMPLATES = templates();

	/**
	 * Wall 1 (111"): dishwasher, sink centred under the window, 15" doors, blind corner. Wall 2
	 * (121"): corner filler, 15" drawers, range under a hood, 15" doors, fridge at the end. Wall
	 * cabinets over the dishwasher and the doors, lined up with them.
	 */
	static Design kitchen() {
		return Design.builder()
				.walls(List.of(wall("w1", 111 * IN), Wall.builder().id("w2").lengthMm(121 * IN).heightMm(96 * IN).angleToNextDegrees(90).build()))
				.cabinets(List.of(
						base("sink", "BASE_SINK", "w1", 24 * IN, 36 * IN),
						base("doors1", "BASE_DOOR", "w1", 60 * IN, 15 * IN),
						base("corner", "BASE_BLIND_CORNER", "w1", 75 * IN, 36 * IN),
						base("drawers", "BASE_DRAWERS", "w2", 25 * IN, 15 * IN),
						base("doors2", "BASE_DOOR", "w2", 70 * IN, 15 * IN),
						upper("upper1", "w1", 0, 24 * IN),
						upper("upper2", "w1", 60 * IN, 15 * IN),
						upper("upper3", "w2", 25 * IN, 15 * IN),
						upper("upper4", "w2", 70 * IN, 15 * IN)))
				.openings(List.of(Opening.builder().id("window").kind(OpeningKind.WINDOW).wallId("w1").offsetMm(24 * IN)
						.widthMm(36 * IN).heightMm(42 * IN).elevationMm(42 * IN).build()))
				.appliances(List.of(
						appliance("dishwasher", ApplianceKind.DISHWASHER, "w1", 0, 24 * IN, 34.5 * IN, 24 * IN, 0),
						appliance("range", ApplianceKind.RANGE, "w2", 40 * IN, 30 * IN, 36 * IN, 26 * IN, 0),
						appliance("hood", ApplianceKind.HOOD, "w2", 40 * IN, 30 * IN, 18 * IN, 20 * IN, 66 * IN),
						appliance("fridge", ApplianceKind.FRIDGE, "w2", 85 * IN, 36 * IN, 70 * IN, 30 * IN, 0)))
				.fillers(List.of(Filler.builder().id("cornerFiller").wallId("w2").offsetMm(24 * IN).widthMm(1 * IN)
						.heightMm(30.5 * IN).depthMm(24 * IN).elevationMm(4 * IN).build()))
				.build();
	}

	@Test
	void aWellLaidKitchenGetsNoRemark() {
		assertThat(check(kitchen())).isEmpty();
	}

	@Test
	void aGapInARunNeedsAFillerOrACabinet() {
		var design = withCabinet(kitchen(), "doors1", cabinet -> cabinet.toBuilder().widthMm(12 * IN).build());

		assertThat(codes(design)).contains(DesignGuidelines.GAP);
		assertThat(remark(design, DesignGuidelines.GAP).getMeasureMm()).isCloseTo(3 * IN, within(0.05));
	}

	@Test
	void aGapAtARunsEndIsASliverToClose() {
		var design = withAppliance(kitchen(), "fridge", fridge -> fridge.toBuilder().offsetMm(83 * IN).build());

		assertThat(codes(design)).contains(DesignGuidelines.GAP_AT_END);
	}

	@Test
	void fillersStayAtRunEndsAndWithinThreeInches() {
		var fillers = new ArrayList<>(kitchen().getFillers());
		fillers.add(Filler.builder().id("middle").wallId("w1").offsetMm(60 * IN).widthMm(4 * IN).heightMm(30.5 * IN)
				.depthMm(24 * IN).elevationMm(4 * IN).build());
		var design = withCabinet(kitchen().toBuilder().fillers(fillers).build(), "doors1",
				cabinet -> cabinet.toBuilder().offsetMm(64 * IN).widthMm(11 * IN).build());

		assertThat(codes(design)).contains(DesignGuidelines.FILLER_MID_RUN, DesignGuidelines.FILLER_WIDTH);
	}

	@Test
	void widthsComeInThreeInchStepsOrFiftyMillimetres() {
		assertThat(codes(withCabinet(kitchen(), "doors1", cabinet -> cabinet.toBuilder().widthMm(400).build())))
				.contains(DesignGuidelines.WIDTH_STEP);
		assertThat(DesignGuidelines.check(kitchen(), TEMPLATES, Units.METRIC)).extracting(Remark::getCode)
				.contains(DesignGuidelines.WIDTH_STEP);
	}

	@Test
	void baseRunsTurnACornerWithABlindCornerCabinetAndAFiller() {
		var plain = withCabinet(kitchen(), "corner", cabinet -> cabinet.toBuilder().templateCode("BASE_DOOR").build());
		assertThat(codes(plain)).contains(DesignGuidelines.CORNER_NOT_BLIND);

		var tight = kitchen().toBuilder().fillers(List.of()).build();
		tight = withCabinet(tight, "drawers", cabinet -> cabinet.toBuilder().offsetMm(24 * IN).widthMm(16 * IN).build());
		assertThat(codes(tight)).contains(DesignGuidelines.CORNER_CLEARANCE);
	}

	@Test
	void theSinkIsCentredUnderTheWindow() {
		var design = kitchen().toBuilder()
				.openings(List.of(kitchen().getOpenings().get(0).toBuilder().offsetMm(30 * IN).build()))
				.build();

		assertThat(remark(design, DesignGuidelines.SINK_NOT_CENTRED).getMeasureMm()).isCloseTo(6 * IN, within(0.05));
	}

	@Test
	void theDishwasherStandsBesideTheSink() {
		var design = withAppliance(kitchen(), "dishwasher", dishwasher -> dishwasher.toBuilder().wallId("w2").offsetMm(97 * IN).build());

		assertThat(codes(design)).contains(DesignGuidelines.DISHWASHER_NOT_BESIDE_SINK);
	}

	@Test
	void theRangeTheSinkAndTheFridgeHaveCounterBesideThem() {
		// The range against the corner run, the drawers moved past the fridge: no counter on its left.
		var design = withAppliance(kitchen().toBuilder().fillers(List.of()).build(), "range",
				range -> range.toBuilder().offsetMm(24 * IN).build());
		design = withCabinet(design, "drawers", cabinet -> cabinet.toBuilder().wallId("w1").offsetMm(0).build());

		assertThat(codes(design)).contains(DesignGuidelines.LANDING_RANGE);
	}

	@Test
	void theWorkTriangleStaysCompact() {
		var design = kitchen().toBuilder()
				.walls(List.of(wall("w1", 111 * IN), kitchen().getWalls().get(1).toBuilder().lengthMm(181 * IN).build()))
				.build();
		design = withAppliance(design, "fridge", fridge -> fridge.toBuilder().offsetMm(145 * IN).build());
		design = withCabinet(design, "doors2", cabinet -> cabinet.toBuilder().widthMm(36 * IN).build());
		design = withCabinet(design, "upper4", cabinet -> cabinet.toBuilder().widthMm(36 * IN).build());
		var cabinets = new ArrayList<>(design.getCabinets());
		cabinets.add(base("more", "BASE_DOOR", "w2", 106 * IN, 36 * IN));
		cabinets.add(base("evenMore", "BASE_DOOR", "w2", 142 * IN, 3 * IN));
		design = design.toBuilder().cabinets(cabinets).build();

		assertThat(codes(design)).contains(DesignGuidelines.WORK_TRIANGLE);
	}

	@Test
	void wallCabinetsLineUpWithWhatIsBelowAndShareATopLine() {
		assertThat(codes(withCabinet(kitchen(), "upper2", cabinet -> cabinet.toBuilder().offsetMm(55 * IN).build())))
				.contains(DesignGuidelines.UPPER_ALIGNMENT);
		assertThat(codes(withCabinet(kitchen(), "upper2", cabinet -> cabinet.toBuilder().elevationMm(56 * IN).build())))
				.contains(DesignGuidelines.UPPER_TOP_LINE);
	}

	@Test
	void aWallCabinetOverTheRangeLeavesRoomToCook() {
		var cabinets = new ArrayList<>(kitchen().getCabinets());
		cabinets.add(upper("overRange", "w2", 40 * IN, 30 * IN));
		var design = kitchen().toBuilder()
				.cabinets(cabinets)
				.appliances(kitchen().getAppliances().stream().filter(appliance -> !appliance.getId().equals("hood")).toList())
				.build();

		assertThat(codes(design)).contains(DesignGuidelines.RANGE_CLEARANCE);
	}

	@Test
	void tallUnitsEndARun() {
		var design = withCabinet(kitchen(), "doors1", cabinet -> cabinet.toBuilder().templateCode("TALL_PANTRY")
				.heightMm(90 * IN).shelfCount(5).build());

		assertThat(codes(design)).contains(DesignGuidelines.TALL_MID_RUN);
	}

	@Test
	void theHoodIsCentredOverTheRangeAndAsWide() {
		assertThat(codes(withAppliance(kitchen(), "hood", hood -> hood.toBuilder().offsetMm(43 * IN).build())))
				.contains(DesignGuidelines.HOOD_OVER_RANGE);
		assertThat(codes(withAppliance(kitchen(), "hood", hood -> hood.toBuilder().wallId("w1").offsetMm(0).build())))
				.contains(DesignGuidelines.HOOD_WITHOUT_RANGE);
	}

	private static List<Remark> check(Design design) {
		return DesignGuidelines.check(design, TEMPLATES, Units.IMPERIAL);
	}

	private static List<String> codes(Design design) {
		return check(design).stream().map(Remark::getCode).toList();
	}

	private static Remark remark(Design design, String code) {
		return check(design).stream().filter(remark -> remark.getCode().equals(code)).findFirst().orElseThrow();
	}

	private static Design withCabinet(Design design, String id, UnaryOperator<Cabinet> change) {
		return design.toBuilder()
				.cabinets(design.getCabinets().stream().map(cabinet -> cabinet.getId().equals(id) ? change.apply(cabinet) : cabinet).toList())
				.build();
	}

	private static Design withAppliance(Design design, String id, UnaryOperator<Appliance> change) {
		return design.toBuilder()
				.appliances(design.getAppliances().stream().map(item -> item.getId().equals(id) ? change.apply(item) : item).toList())
				.build();
	}

	private static Wall wall(String id, double length) {
		return Wall.builder().id(id).lengthMm(length).heightMm(96 * IN).angleToNextDegrees(90).build();
	}

	private static Cabinet base(String id, String code, String wallId, double offset, double width) {
		return Cabinet.builder().id(id).templateCode(code).templateVersion(2).wallId(wallId).offsetMm(offset).widthMm(width)
				.heightMm(34.5 * IN).depthMm(24 * IN).elevationMm(0).shelfCount(code.equals("BASE_DOOR") ? 1 : 0).build();
	}

	private static Cabinet upper(String id, String wallId, double offset, double width) {
		return Cabinet.builder().id(id).templateCode("WALL_DOOR").templateVersion(2).wallId(wallId).offsetMm(offset).widthMm(width)
				.heightMm(30 * IN).depthMm(12 * IN).elevationMm(54 * IN).shelfCount(2).build();
	}

	private static Appliance appliance(String id, ApplianceKind kind, String wallId, double offset, double width, double height,
			double depth, double elevation) {
		return Appliance.builder().id(id).kind(kind).wallId(wallId).offsetMm(offset).widthMm(width).heightMm(height)
				.depthMm(depth).elevationMm(elevation).build();
	}

	private static List<CabinetTemplate> templates() {
		try (var input = new ClassPathResource("cabinet-templates.json").getInputStream()) {
			return JsonConfiguration.configure(JsonMapper.builder()).build().readValue(input, new TypeReference<List<CabinetTemplate>>() {
			});
		} catch (IOException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
