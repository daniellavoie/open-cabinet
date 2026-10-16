package dev.daniellavoie.opencabinet.cutlist.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.catalog.domain.port.api.CatalogDomain;
import dev.daniellavoie.opencabinet.cutlist.domain.model.BomLine;
import dev.daniellavoie.opencabinet.cutlist.domain.model.CutListRow;
import dev.daniellavoie.opencabinet.cutlist.domain.model.MaterialNesting;
import dev.daniellavoie.opencabinet.cutlist.domain.model.OperationCounts;
import dev.daniellavoie.opencabinet.cutlist.domain.model.Part;
import dev.daniellavoie.opencabinet.cutlist.domain.model.ProjectOutput;
import dev.daniellavoie.opencabinet.cutlist.domain.port.api.CutListDomain;
import dev.daniellavoie.opencabinet.member.domain.port.api.MemberDomain;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.Project;
import dev.daniellavoie.opencabinet.project.domain.port.api.ProjectDomain;
import dev.daniellavoie.opencabinet.shop.domain.model.ConstructionStandard;
import dev.daniellavoie.opencabinet.shop.domain.port.backend.ShopRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultCutListDomain implements CutListDomain {
	/**
	 * Edge banding overhangs each edge by this much before trimming.
	 */
	static final double EDGE_BANDING_OVERHANG_MM = 25;
	static final BigDecimal EDGE_BANDING_WASTE = new BigDecimal("1.10");
	static final double FOOT_MM = 304.8;

	MemberDomain memberDomain;
	ProjectDomain projectDomain;
	CatalogDomain catalogDomain;
	ShopRepository shopRepository;

	@Override
	public ProjectOutput output(UUID projectId, Principal principal) {
		var shopId = memberDomain.current(principal).getShopId();

		return output(projectDomain.findById(projectId, shopId),
				shopRepository.findById(shopId).getSettings().getConstructionStandard(),
				catalogDomain.findByShop(shopId));
	}

	@Override
	public ProjectOutput output(Project project, ConstructionStandard standard, List<Material> catalog) {
		Map<UUID, Material> materials = catalog.stream()
				.collect(Collectors.toMap(Material::getId, Function.identity()));
		Map<String, CabinetTemplate> templates = projectDomain.templates().stream()
				.collect(Collectors.toMap(CabinetTemplate::getCode, Function.identity()));

		var cabinets = readingOrder(project);
		var labels = new LinkedHashMap<String, String>();
		IntStream.range(0, cabinets.size()).forEach(index -> labels.put(cabinets.get(index).getId(), "C" + (index + 1)));

		var parts = new ArrayList<Part>();
		var hardware = new LinkedHashMap<UUID, Integer>();
		for (var cabinet : cabinets) {
			var set = PartGenerator.generate(cabinet, templates.get(cabinet.getTemplateCode()),
					labels.get(cabinet.getId()), standard, materials);
			parts.addAll(set.parts());
			set.hardware().forEach((materialId, quantity) -> hardware.merge(materialId, quantity, Integer::sum));
		}

		var nesting = parts.stream()
				.map(Part::getMaterialId)
				.collect(Collectors.toCollection(LinkedHashSet::new))
				.stream()
				.map(materialId -> Nester.nest(materials.get(materialId),
						parts.stream().filter(part -> part.getMaterialId().equals(materialId)).toList(),
						standard.getKerfMm(), standard.getSheetTrimMm()))
				.toList();

		var edgeBanding = edgeBandingMeters(parts);
		var billOfMaterials = new ArrayList<BomLine>();
		nesting.forEach(materialNesting -> billOfMaterials.add(line(materials.get(materialNesting.getMaterialId()),
				BigDecimal.valueOf(materialNesting.getSheets().size()))));
		edgeBanding.forEach((materialId, meters) -> billOfMaterials.add(line(materials.get(materialId), meters)));
		hardware.forEach((materialId, quantity) -> billOfMaterials.add(line(materials.get(materialId),
				BigDecimal.valueOf(quantity))));

		return ProjectOutput.builder()
				.cabinetLabels(labels)
				.cutList(cutList(parts, materials))
				.nesting(nesting)
				.billOfMaterials(billOfMaterials)
				.materialsTotal(billOfMaterials.stream().map(BomLine::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add))
				.operations(OperationCounts.builder()
						.sheets(nesting.stream().mapToInt(materialNesting -> materialNesting.getSheets().size()).sum())
						.edgeBandingMeters(edgeBanding.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add))
						.cabinets(cabinets.size())
						.hardwareItems(hardware.values().stream().mapToInt(Integer::intValue).sum())
						.linearFeet(BigDecimal.valueOf(cabinets.stream().mapToDouble(Cabinet::getWidthMm).sum() / FOOT_MM)
								.setScale(2, RoundingMode.HALF_UP))
						.build())
				.build();
	}

	/**
	 * Cabinets ordered by Wall, then from the Wall's start, then from the floor up.
	 */
	static List<Cabinet> readingOrder(Project project) {
		var walls = project.getDesign().getWalls();
		var wallIndex = IntStream.range(0, walls.size())
				.boxed()
				.collect(Collectors.toMap(index -> walls.get(index).getId(), Function.identity()));

		return Optional.ofNullable(project.getDesign().getCabinets())
				.orElse(List.of())
				.stream()
				.sorted(Comparator.<Cabinet>comparingInt(cabinet -> wallIndex.getOrDefault(cabinet.getWallId(),
						Integer.MAX_VALUE))
						.thenComparingDouble(Cabinet::getOffsetMm)
						.thenComparingDouble(Cabinet::getElevationMm))
				.toList();
	}

	static Map<UUID, BigDecimal> edgeBandingMeters(List<Part> parts) {
		var millimetres = new LinkedHashMap<UUID, Double>();
		for (var part : parts) {
			if (part.getEdgeBandingId() == null) {
				continue;
			}
			var length = part.getBandedLengthEdges() * (part.getLengthMm() + 2 * EDGE_BANDING_OVERHANG_MM)
					+ part.getBandedWidthEdges() * (part.getWidthMm() + 2 * EDGE_BANDING_OVERHANG_MM);
			millimetres.merge(part.getEdgeBandingId(), length, Double::sum);
		}

		var meters = new LinkedHashMap<UUID, BigDecimal>();
		millimetres.forEach((materialId, length) -> meters.put(materialId,
				BigDecimal.valueOf(length / 1000)
						.multiply(EDGE_BANDING_WASTE)
						.setScale(1, RoundingMode.CEILING)));
		return meters;
	}

	private static List<CutListRow> cutList(List<Part> parts, Map<UUID, Material> materials) {
		var rows = new LinkedHashMap<List<Object>, List<Part>>();
		parts.forEach(part -> rows.computeIfAbsent(List.of(part.getRole(), part.getMaterialId(), part.getLengthMm(),
				part.getWidthMm(), part.getBandedLengthEdges(), part.getBandedWidthEdges(),
				Optional.ofNullable(part.getEdgeBandingId()).map(UUID::toString).orElse("")),
				key -> new ArrayList<>()).add(part));

		return rows.values()
				.stream()
				.map(group -> {
					var part = group.get(0);
					return CutListRow.builder()
							.role(part.getRole())
							.materialId(part.getMaterialId())
							.materialName(materials.get(part.getMaterialId()).getName())
							.lengthMm(part.getLengthMm())
							.widthMm(part.getWidthMm())
							.thicknessMm(part.getThicknessMm())
							.grain(part.isGrain())
							.bandedLengthEdges(part.getBandedLengthEdges())
							.bandedWidthEdges(part.getBandedWidthEdges())
							.edgeBandingId(part.getEdgeBandingId())
							.edgeBandingName(Optional.ofNullable(part.getEdgeBandingId())
									.map(materials::get)
									.map(Material::getName)
									.orElse(null))
							.quantity(group.size())
							.cabinetLabels(group.stream().map(Part::getCabinetLabel).distinct().toList())
							.build();
				})
				.sorted(Comparator.comparing(CutListRow::getMaterialName)
						.thenComparing(CutListRow::getRole)
						.thenComparing(Comparator.comparingDouble(CutListRow::getLengthMm).reversed())
						.thenComparing(Comparator.comparingDouble(CutListRow::getWidthMm).reversed()))
				.toList();
	}

	private static BomLine line(Material material, BigDecimal quantity) {
		return BomLine.builder()
				.materialId(material.getId())
				.kind(material.getKind())
				.name(material.getName())
				.sku(material.getSku())
				.quantity(quantity)
				.unitPrice(material.getPrice())
				.total(material.getPrice().multiply(quantity).setScale(2, RoundingMode.HALF_UP))
				.build();
	}
}
