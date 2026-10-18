package dev.daniellavoie.opencabinet.project.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import dev.daniellavoie.opencabinet.project.domain.model.Appliance;
import dev.daniellavoie.opencabinet.project.domain.model.ApplianceKind;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.Filler;
import dev.daniellavoie.opencabinet.project.domain.model.Opening;
import dev.daniellavoie.opencabinet.project.domain.model.OpeningKind;
import dev.daniellavoie.opencabinet.project.domain.model.Wall;

/**
 * The editor's edits to a Design, each returning a new one: the same operations as
 * {@code design-edits.ts}, kept in step by {@code design-edits/cases.json}. New Walls, Cabinets
 * and items get the id they are given, so replaying an edit recreates the same ids.
 */
public final class DesignEdits {
	static final double INCH = 25.4;
	static final double CORNER_FILLER_MM = 1 * INCH;

	public static final Map<OpeningKind, Opening> OPENING_DEFAULTS = Map.of(
			OpeningKind.WINDOW, Opening.builder().widthMm(36 * INCH).heightMm(42 * INCH).elevationMm(42 * INCH).build(),
			OpeningKind.DOORWAY, Opening.builder().widthMm(36 * INCH).heightMm(80 * INCH).elevationMm(0).build());

	public static final Map<ApplianceKind, Appliance> APPLIANCE_DEFAULTS = Map.of(
			ApplianceKind.FRIDGE, appliance(36, 70, 30, 0),
			ApplianceKind.RANGE, appliance(30, 36, 26, 0),
			ApplianceKind.DISHWASHER, appliance(24, 34.5, 24, 0),
			ApplianceKind.HOOD, appliance(30, 18, 20, 66));

	/**
	 * A base-height Filler: as tall as a base Cabinet's box, above its toe kick.
	 */
	public static final Filler FILLER_DEFAULTS = Filler.builder()
			.widthMm(3 * INCH).heightMm(30.5 * INCH).depthMm(24 * INCH).elevationMm(4 * INCH).build();

	/**
	 * Changes to an Opening, Appliance or Filler; null leaves a value as it is.
	 */
	public record ItemChanges(String wallId, Double offsetMm, Double widthMm, Double heightMm, Double depthMm,
			Double elevationMm) {
	}

	private DesignEdits() {
	}

	public static Design addWall(Design design, String id) {
		var walls = walls(design);
		var last = walls.isEmpty() ? null : walls.get(walls.size() - 1);
		var wall = Wall.builder()
				.id(id)
				.lengthMm(last == null ? 3657.6 : last.getLengthMm())
				.heightMm(last == null ? 2438.4 : last.getHeightMm())
				.angleToNextDegrees(90)
				.build();
		return design.toBuilder().walls(append(walls, wall)).build();
	}

	public static Design updateWall(Design design, String wallId, UnaryOperator<Wall> change) {
		return design.toBuilder()
				.walls(walls(design).stream()
						.map(wall -> wall.getId().equals(wallId) ? change.apply(wall).toBuilder().id(wall.getId()).build() : wall)
						.toList())
				.build();
	}

	/**
	 * Removes a Wall and everything on it. A Room keeps at least one Wall.
	 */
	public static Design removeWall(Design design, String wallId) {
		if (walls(design).size() <= 1) {
			return design;
		}
		return design.toBuilder()
				.walls(walls(design).stream().filter(wall -> !wall.getId().equals(wallId)).toList())
				.cabinets(cabinets(design).stream().filter(cabinet -> !cabinet.getWallId().equals(wallId)).toList())
				.openings(openings(design).stream().filter(item -> !item.getWallId().equals(wallId)).toList())
				.appliances(appliances(design).stream().filter(item -> !item.getWallId().equals(wallId)).toList())
				.fillers(fillers(design).stream().filter(item -> !item.getWallId().equals(wallId)).toList())
				.build();
	}

	public static Design addFiller(Design design, String wallId, String id) {
		var filler = FILLER_DEFAULTS.toBuilder().id(id).wallId(wallId).offsetMm(0).build();
		return design.toBuilder().fillers(append(fillers(design), filler)).build();
	}

	public static Design addOpening(Design design, OpeningKind kind, String wallId, String id) {
		var opening = OPENING_DEFAULTS.get(kind).toBuilder().id(id).kind(kind).wallId(wallId).offsetMm(0).build();
		return design.toBuilder().openings(append(openings(design), opening)).build();
	}

	public static Design addAppliance(Design design, ApplianceKind kind, String wallId, String id) {
		var appliance = APPLIANCE_DEFAULTS.get(kind).toBuilder().id(id).kind(kind).wallId(wallId).offsetMm(0).build();
		return design.toBuilder().appliances(append(appliances(design), appliance)).build();
	}

	/**
	 * Changes the Opening, Appliance or Filler with this id.
	 */
	public static Design updateItem(Design design, String itemId, ItemChanges changes) {
		BinaryOperator<Double> pick = (change, current) -> change != null ? change : current;
		return design.toBuilder()
				.openings(openings(design).stream()
						.map(item -> !item.getId().equals(itemId) ? item : item.toBuilder()
								.wallId(Optional.ofNullable(changes.wallId()).orElse(item.getWallId()))
								.offsetMm(pick.apply(changes.offsetMm(), item.getOffsetMm()))
								.widthMm(pick.apply(changes.widthMm(), item.getWidthMm()))
								.heightMm(pick.apply(changes.heightMm(), item.getHeightMm()))
								.elevationMm(pick.apply(changes.elevationMm(), item.getElevationMm()))
								.build())
						.toList())
				.appliances(appliances(design).stream()
						.map(item -> !item.getId().equals(itemId) ? item : item.toBuilder()
								.wallId(Optional.ofNullable(changes.wallId()).orElse(item.getWallId()))
								.offsetMm(pick.apply(changes.offsetMm(), item.getOffsetMm()))
								.widthMm(pick.apply(changes.widthMm(), item.getWidthMm()))
								.heightMm(pick.apply(changes.heightMm(), item.getHeightMm()))
								.depthMm(pick.apply(changes.depthMm(), item.getDepthMm()))
								.elevationMm(pick.apply(changes.elevationMm(), item.getElevationMm()))
								.build())
						.toList())
				.fillers(fillers(design).stream()
						.map(item -> !item.getId().equals(itemId) ? item : item.toBuilder()
								.wallId(Optional.ofNullable(changes.wallId()).orElse(item.getWallId()))
								.offsetMm(pick.apply(changes.offsetMm(), item.getOffsetMm()))
								.widthMm(pick.apply(changes.widthMm(), item.getWidthMm()))
								.heightMm(pick.apply(changes.heightMm(), item.getHeightMm()))
								.depthMm(pick.apply(changes.depthMm(), item.getDepthMm()))
								.elevationMm(pick.apply(changes.elevationMm(), item.getElevationMm()))
								.build())
						.toList())
				.build();
	}

	public static Design removeItem(Design design, String itemId) {
		return design.toBuilder()
				.openings(openings(design).stream().filter(item -> !item.getId().equals(itemId)).toList())
				.appliances(appliances(design).stream().filter(item -> !item.getId().equals(itemId)).toList())
				.fillers(fillers(design).stream().filter(item -> !item.getId().equals(itemId)).toList())
				.build();
	}

	/**
	 * Adds a Cabinet from a template, at its default size, to a Wall, at the first spot along the
	 * Wall where it breaks no Construction Rule (or at the Wall's start if none): the start, just
	 * past another Cabinet, Appliance, Opening or Filler, or past what stands on a neighbouring
	 * Wall's corner, leaving room for a filler.
	 */
	public static Design addCabinet(Design design, CabinetTemplate template, String wallId, List<CabinetTemplate> templates,
			String id) {
		var cabinet = Cabinet.builder()
				.templateCode(template.getCode())
				.templateVersion(template.getVersion())
				.offsetMm(0)
				.widthMm(template.getDefaultWidthMm())
				.heightMm(template.getDefaultHeightMm())
				.depthMm(template.getDefaultDepthMm())
				.elevationMm(template.getDefaultElevationMm())
				.shelfCount(template.getDefaultShelfCount())
				.id(id)
				.wallId(wallId)
				.build();

		var onWall = Stream.of(
				cabinets(design).stream().filter(other -> other.getWallId().equals(wallId))
						.map(other -> other.getOffsetMm() + other.getWidthMm()),
				appliances(design).stream().filter(other -> other.getWallId().equals(wallId))
						.map(other -> other.getOffsetMm() + other.getWidthMm()),
				openings(design).stream().filter(other -> other.getWallId().equals(wallId))
						.map(other -> other.getOffsetMm() + other.getWidthMm()),
				fillers(design).stream().filter(other -> other.getWallId().equals(wallId))
						.map(other -> other.getOffsetMm() + other.getWidthMm()))
				.flatMap(offsets -> offsets);
		var pastCorner = Stream.concat(
				cabinets(design).stream().filter(other -> !other.getWallId().equals(wallId)).map(Cabinet::getDepthMm),
				appliances(design).stream().filter(other -> !other.getWallId().equals(wallId)).map(Appliance::getDepthMm))
				.map(depth -> depth + CORNER_FILLER_MM);
		var candidates = Stream.concat(Stream.of(0.0), Stream.concat(onWall, pastCorner)).sorted().toList();

		for (var offsetMm : candidates) {
			var placed = cabinet.toBuilder().offsetMm(offsetMm).build();
			var candidate = design.toBuilder().cabinets(append(cabinets(design), placed)).build();
			if (ConstructionRules.check(candidate, templates).stream().noneMatch(violation -> id.equals(violation.getCabinetId()))) {
				return candidate;
			}
		}

		return design.toBuilder().cabinets(append(cabinets(design), cabinet)).build();
	}

	public static Design updateCabinet(Design design, String cabinetId, UnaryOperator<Cabinet> change) {
		return design.toBuilder()
				.cabinets(cabinets(design).stream()
						.map(cabinet -> cabinet.getId().equals(cabinetId)
								? change.apply(cabinet).toBuilder().id(cabinet.getId()).build()
								: cabinet)
						.toList())
				.build();
	}

	/**
	 * Adds a divider in the middle of the Cabinet's widest section.
	 */
	public static Design addDivider(Design design, String cabinetId) {
		return withDividers(design, cabinetId, (dividers, cabinet) -> {
			var bounds = new ArrayList<Double>();
			bounds.add(0.0);
			bounds.addAll(dividers);
			bounds.add(cabinet.getWidthMm());
			var widest = 0;
			for (var index = 1; index < bounds.size() - 1; index++) {
				if (bounds.get(index + 1) - bounds.get(index) > bounds.get(widest + 1) - bounds.get(widest)) {
					widest = index;
				}
			}
			var middle = Math.round((bounds.get(widest) + bounds.get(widest + 1)) / 2 * 10) / 10.0;
			var result = new ArrayList<>(dividers.subList(0, widest));
			result.add(middle);
			result.addAll(dividers.subList(widest, dividers.size()));
			return result;
		});
	}

	public static Design moveDivider(Design design, String cabinetId, int index, double positionMm) {
		return withDividers(design, cabinetId, (dividers, cabinet) -> {
			var result = new ArrayList<>(dividers);
			if (index >= 0 && index < result.size()) {
				result.set(index, positionMm);
			}
			return result;
		});
	}

	public static Design removeDivider(Design design, String cabinetId, int index) {
		return withDividers(design, cabinetId, (dividers, cabinet) -> {
			var result = new ArrayList<>(dividers);
			if (index >= 0 && index < result.size()) {
				result.remove(index);
			}
			return result;
		});
	}

	public static Design removeCabinet(Design design, String cabinetId) {
		return design.toBuilder()
				.cabinets(cabinets(design).stream().filter(cabinet -> !cabinet.getId().equals(cabinetId)).toList())
				.build();
	}

	private interface DividerChange {
		List<Double> apply(List<Double> dividers, Cabinet cabinet);
	}

	private static Design withDividers(Design design, String cabinetId, DividerChange change) {
		return design.toBuilder()
				.cabinets(cabinets(design).stream()
						.map(cabinet -> cabinet.getId().equals(cabinetId)
								? cabinet.toBuilder().dividerPositionsMm(List.copyOf(change.apply(cabinet.dividers(), cabinet))).build()
								: cabinet)
						.toList())
				.build();
	}

	private static Appliance appliance(double widthInches, double heightInches, double depthInches, double elevationInches) {
		return Appliance.builder()
				.widthMm(widthInches * INCH)
				.heightMm(heightInches * INCH)
				.depthMm(depthInches * INCH)
				.elevationMm(elevationInches * INCH)
				.build();
	}

	private static <T> List<T> append(List<T> list, T item) {
		var result = new ArrayList<>(list);
		result.add(item);
		return List.copyOf(result);
	}

	static List<Wall> walls(Design design) {
		return Optional.ofNullable(design.getWalls()).orElse(List.of());
	}

	static List<Cabinet> cabinets(Design design) {
		return Optional.ofNullable(design.getCabinets()).orElse(List.of());
	}

	static List<Opening> openings(Design design) {
		return Optional.ofNullable(design.getOpenings()).orElse(List.of());
	}

	static List<Appliance> appliances(Design design) {
		return Optional.ofNullable(design.getAppliances()).orElse(List.of());
	}

	static List<Filler> fillers(Design design) {
		return Optional.ofNullable(design.getFillers()).orElse(List.of());
	}
}
