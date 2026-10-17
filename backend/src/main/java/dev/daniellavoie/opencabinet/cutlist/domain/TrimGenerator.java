package dev.daniellavoie.opencabinet.cutlist.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.cutlist.domain.model.Part;
import dev.daniellavoie.opencabinet.cutlist.domain.model.PartRole;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetType;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.Filler;
import dev.daniellavoie.opencabinet.project.domain.model.Wall;
import dev.daniellavoie.opencabinet.shop.domain.model.ConstructionStandard;

/**
 * The Trim that belongs to the Project rather than to one Cabinet: Fillers (in Front material), a toe
 * kick (in carcass material) along each run of Cabinets standing on the floor, and crown moulding along
 * each run of Cabinets that ask for it.
 * Finished end panels belong to their Cabinet ({@link PartGenerator}).
 */
final class TrimGenerator {
	/**
	 * Cabinets this close or closer, end to end, form one run.
	 */
	static final double RUN_GAP_MM = 1;
	static final BigDecimal MOULDING_WASTE = new BigDecimal("1.10");

	record TrimSet(List<Part> parts, Map<UUID, BigDecimal> mouldingMeters) {
	}

	private TrimGenerator() {
	}

	static TrimSet generate(Design design, Map<String, String> labels, Map<String, CabinetTemplate> templates,
			ConstructionStandard standard, Map<UUID, Material> catalog) {
		var walls = Optional.ofNullable(design.getWalls()).orElse(List.of());
		var cabinets = Optional.ofNullable(design.getCabinets()).orElse(List.of());
		var front = catalog.get(standard.getFrontMaterialId());
		var carcass = catalog.get(standard.getCarcassMaterialId());
		var parts = new ArrayList<Part>();

		var fillers = readingOrder(Optional.ofNullable(design.getFillers()).orElse(List.of()), walls);
		IntStream.range(0, fillers.size()).forEach(index -> {
			var filler = fillers.get(index);
			parts.add(part(PartRole.FILLER, front, "F" + (index + 1), filler.getHeightMm(), filler.getWidthMm()));
		});

		var maxLength = Optional.ofNullable(carcass.getLengthMm()).orElse(Double.MAX_VALUE) - 2 * standard.getSheetTrimMm();
		for (var run : runs(walls, cabinets, cabinet -> type(cabinet, templates).standsOnFloor(), false)) {
			var length = runLength(run);
			var pieces = (int) Math.ceil(length / maxLength);
			for (int piece = 0; piece < pieces; piece++) {
				parts.add(part(PartRole.TOE_KICK, carcass, runLabel(run, labels), length / pieces,
						standard.getToeKickHeightMm()));
			}
		}

		var mouldingMeters = new LinkedHashMap<UUID, BigDecimal>();
		if (standard.getCrownMouldingId() != null) {
			var wallsById = walls.stream().collect(Collectors.toMap(Wall::getId, Function.identity(),
					(first, second) -> first));
			var millimetres = 0d;
			for (var run : runs(walls, cabinets,
					cabinet -> cabinet.isCrown() && type(cabinet, templates) != CabinetType.BASE, true)) {
				var first = run.get(0);
				var last = run.get(run.size() - 1);
				millimetres += runLength(run);
				if (first.getOffsetMm() > RUN_GAP_MM) {
					millimetres += first.getDepthMm();
				}
				if (last.getOffsetMm() + last.getWidthMm() < wallsById.get(last.getWallId()).getLengthMm() - RUN_GAP_MM) {
					millimetres += last.getDepthMm();
				}
			}
			if (millimetres > 0) {
				mouldingMeters.put(standard.getCrownMouldingId(), BigDecimal.valueOf(millimetres / 1000)
						.multiply(MOULDING_WASTE)
						.setScale(1, RoundingMode.CEILING));
			}
		}

		return new TrimSet(parts, mouldingMeters);
	}

	/**
	 * Runs of matching Cabinets standing end to end on the same Wall, Wall by Wall, each from the
	 * Wall's start. With {@code sameTop}, a run also breaks where the tops step.
	 */
	static List<List<Cabinet>> runs(List<Wall> walls, List<Cabinet> cabinets, Predicate<Cabinet> matching,
			boolean sameTop) {
		var runs = new ArrayList<List<Cabinet>>();
		for (var wall : walls) {
			var onWall = cabinets.stream()
					.filter(cabinet -> cabinet.getWallId().equals(wall.getId()) && matching.test(cabinet))
					.sorted(Comparator.comparingDouble(Cabinet::getOffsetMm))
					.toList();
			List<Cabinet> run = null;
			for (var cabinet : onWall) {
				if (run != null && continues(run.get(run.size() - 1), cabinet, sameTop)) {
					run.add(cabinet);
				} else {
					run = new ArrayList<>(List.of(cabinet));
					runs.add(run);
				}
			}
		}
		return runs;
	}

	private static boolean continues(Cabinet previous, Cabinet next, boolean sameTop) {
		var touching = next.getOffsetMm() <= previous.getOffsetMm() + previous.getWidthMm() + RUN_GAP_MM;
		var top = previous.getElevationMm() + previous.getHeightMm();
		return touching && (!sameTop || Math.abs(next.getElevationMm() + next.getHeightMm() - top) <= RUN_GAP_MM);
	}

	private static double runLength(List<Cabinet> run) {
		var first = run.get(0);
		return run.stream().mapToDouble(cabinet -> cabinet.getOffsetMm() + cabinet.getWidthMm()).max().orElse(0)
				- first.getOffsetMm();
	}

	private static String runLabel(List<Cabinet> run, Map<String, String> labels) {
		var first = labels.get(run.get(0).getId());
		var last = labels.get(run.get(run.size() - 1).getId());
		return run.size() == 1 ? first : first + "–" + last;
	}

	private static CabinetType type(Cabinet cabinet, Map<String, CabinetTemplate> templates) {
		return Optional.ofNullable(templates.get(cabinet.getTemplateCode()))
				.map(CabinetTemplate::getType)
				.orElse(CabinetType.WALL);
	}

	private static List<Filler> readingOrder(List<Filler> fillers, List<Wall> walls) {
		var wallIndex = IntStream.range(0, walls.size()).boxed()
				.collect(Collectors.toMap(index -> walls.get(index).getId(), Function.identity(),
						(first, second) -> first));
		return fillers.stream()
				.sorted(Comparator.<Filler>comparingInt(filler -> wallIndex.getOrDefault(filler.getWallId(),
						Integer.MAX_VALUE))
						.thenComparingDouble(Filler::getOffsetMm)
						.thenComparingDouble(Filler::getElevationMm))
				.toList();
	}

	/**
	 * A Trim Part {@code lengthMm} by {@code widthMm}, the grain along its length, no edge banding.
	 */
	private static Part part(PartRole role, Material material, String label, double lengthMm, double widthMm) {
		var keepOrientation = material.isGrain() || lengthMm >= widthMm;
		return Part.builder()
				.cabinetLabel(label)
				.role(role)
				.materialId(material.getId())
				.lengthMm(PartGenerator.round(keepOrientation ? lengthMm : widthMm))
				.widthMm(PartGenerator.round(keepOrientation ? widthMm : lengthMm))
				.thicknessMm(material.getThicknessMm())
				.grain(material.isGrain())
				.build();
	}
}
