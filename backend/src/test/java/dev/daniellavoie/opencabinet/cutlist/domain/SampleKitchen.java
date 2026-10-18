package dev.daniellavoie.opencabinet.cutlist.domain;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.catalog.domain.port.api.CatalogDomain;
import dev.daniellavoie.opencabinet.cutlist.domain.model.ProjectOutput;
import dev.daniellavoie.opencabinet.member.domain.port.api.MemberDomain;
import dev.daniellavoie.opencabinet.project.domain.model.Appliance;
import dev.daniellavoie.opencabinet.project.domain.model.ApplianceKind;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.Filler;
import dev.daniellavoie.opencabinet.project.domain.model.Opening;
import dev.daniellavoie.opencabinet.project.domain.model.OpeningKind;
import dev.daniellavoie.opencabinet.project.domain.model.Project;
import dev.daniellavoie.opencabinet.project.domain.model.Side;
import dev.daniellavoie.opencabinet.project.domain.model.Wall;
import dev.daniellavoie.opencabinet.project.domain.port.api.ProjectDomain;
import dev.daniellavoie.opencabinet.shop.domain.model.ConstructionStandard;
import dev.daniellavoie.opencabinet.shop.domain.port.backend.ShopRepository;

/**
 * An L-shaped kitchen built from the starter templates, with its real Cut List, for tests of what
 * is drawn or printed from a whole Project. Wall 1: a base Cabinet with a divider, a dishwasher, a
 * sink base under a window, a drawer bank and a blind corner, with two wall Cabinets. Wall 2: a
 * filler past the corner, two base Cabinets, a fridge and a pantry. Wall 3: a doorway.
 */
public final class SampleKitchen {
	private static final List<CabinetTemplate> TEMPLATES = readTemplates();

	private SampleKitchen() {
	}

	public static List<CabinetTemplate> templates() {
		return TEMPLATES;
	}

	public static ConstructionStandard standard() {
		return CutListFixtures.STANDARD;
	}

	public static List<Material> catalog() {
		return CutListFixtures.CATALOG;
	}

	public static Design design() {
		return Design.builder()
				.walls(List.of(
						Wall.builder().id("w1").lengthMm(3657.6).heightMm(2438.4).angleToNextDegrees(90).build(),
						Wall.builder().id("w2").lengthMm(3048).heightMm(2438.4).angleToNextDegrees(90).build(),
						Wall.builder().id("w3").lengthMm(3657.6).heightMm(2438.4).angleToNextDegrees(0).build()))
				.cabinets(List.of(
						cabinet("base", "BASE_DOOR", "w1", 0, 457.2, 0).toBuilder()
								.dividerPositionsMm(List.of(228.6)).build(),
						cabinet("sink", "BASE_SINK", "w1", 1066.8, 914.4, 0),
						cabinet("drawers", "BASE_DRAWERS", "w1", 1981.2, 609.6, 0),
						cabinet("corner", "BASE_BLIND_CORNER", "w1", 2590.8, 1066.8, 0).toBuilder()
								.blindSide(Side.RIGHT).build(),
						cabinet("upperLeft", "WALL_DOOR", "w1", 0, 762, 1371.6).toBuilder().depthMm(304.8)
								.heightMm(762).crown(true).build(),
						cabinet("upperRight", "WALL_DOOR", "w1", 2286, 762, 1371.6).toBuilder().depthMm(304.8)
								.heightMm(762).crown(true).build(),
						cabinet("afterCorner", "BASE_DOOR", "w2", 635, 457.2, 0),
						cabinet("beforeFridge", "BASE_DOOR", "w2", 1092.2, 431.8, 0),
						cabinet("pantry", "TALL_PANTRY", "w2", 2438.4, 609.6, 0).toBuilder().heightMm(2286)
								.finishedRight(true).build()))
				.openings(List.of(
						Opening.builder().id("window").kind(OpeningKind.WINDOW).wallId("w1").offsetMm(1066.8)
								.widthMm(914.4).elevationMm(1066.8).heightMm(1066.8).build(),
						Opening.builder().id("doorway").kind(OpeningKind.DOORWAY).wallId("w3").offsetMm(914.4)
								.widthMm(914.4).elevationMm(0).heightMm(2032).build()))
				.appliances(List.of(
						Appliance.builder().id("dishwasher").kind(ApplianceKind.DISHWASHER).wallId("w1").offsetMm(457.2)
								.widthMm(609.6).heightMm(876.3).depthMm(609.6).elevationMm(0).build(),
						Appliance.builder().id("fridge").kind(ApplianceKind.FRIDGE).wallId("w2").offsetMm(1524)
								.widthMm(914.4).heightMm(1778).depthMm(762).elevationMm(0).build()))
				.fillers(List.of(
						Filler.builder().id("filler").wallId("w2").offsetMm(609.6).widthMm(25.4).heightMm(876.3)
								.depthMm(609.6).elevationMm(0).build()))
				.build();
	}

	public static Project project() {
		return Project.builder().id(UUID.randomUUID()).clientId(UUID.randomUUID()).name("Cuisine Tremblay")
				.design(design()).build();
	}

	/**
	 * The Project's real Cut List and labels, computed by the cut list domain.
	 */
	public static ProjectOutput output() {
		var projectDomain = mock(ProjectDomain.class);
		when(projectDomain.templates(any(Project.class))).thenReturn(TEMPLATES);
		return new DefaultCutListDomain(mock(MemberDomain.class), projectDomain, mock(CatalogDomain.class),
				mock(ShopRepository.class)).output(project(), standard(), catalog());
	}

	private static Cabinet cabinet(String id, String code, String wallId, double offset, double width,
			double elevation) {
		var template = TEMPLATES.stream().filter(candidate -> candidate.getCode().equals(code)).findFirst().orElseThrow();
		return Cabinet.builder().id(id).templateCode(code).templateVersion(template.getVersion()).wallId(wallId)
				.offsetMm(offset).widthMm(width).heightMm(template.getDefaultHeightMm())
				.depthMm(template.getDefaultDepthMm()).elevationMm(elevation)
				.shelfCount(Math.min(1, template.getMaxShelfCount())).build();
	}

	private static List<CabinetTemplate> readTemplates() {
		try (var input = new ClassPathResource("cabinet-templates.json").getInputStream()) {
			return new ObjectMapper().readValue(input, new TypeReference<List<CabinetTemplate>>() {
			});
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
}
