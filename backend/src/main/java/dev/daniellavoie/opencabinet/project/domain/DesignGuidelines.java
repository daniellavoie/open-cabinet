package dev.daniellavoie.opencabinet.project.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import dev.daniellavoie.opencabinet.project.domain.model.ApplianceKind;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetType;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.OpeningKind;
import dev.daniellavoie.opencabinet.project.domain.model.Remark;
import dev.daniellavoie.opencabinet.project.domain.model.Wall;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

/**
 * The Design Guidelines: what makes a kitchen work and look right, checked wall by wall. A design
 * that does not follow one gets a {@link Remark}; it never blocks, unlike the Construction Rules.
 * Lengths follow the Shop's units: inches for imperial Shops, round millimetres for metric ones.
 */
public final class DesignGuidelines {
	public static final String GAP = "GAP";
	public static final String GAP_AT_END = "GAP_AT_END";
	public static final String FILLER_WIDTH = "FILLER_WIDTH";
	public static final String FILLER_MID_RUN = "FILLER_MID_RUN";
	public static final String WIDTH_STEP = "WIDTH_STEP";
	public static final String CORNER_NOT_BLIND = "CORNER_NOT_BLIND";
	public static final String CORNER_CLEARANCE = "CORNER_CLEARANCE";
	public static final String UPPER_CORNER_CLEARANCE = "UPPER_CORNER_CLEARANCE";
	public static final String SINK_NOT_CENTRED = "SINK_NOT_CENTRED";
	public static final String DISHWASHER_NOT_BESIDE_SINK = "DISHWASHER_NOT_BESIDE_SINK";
	public static final String DISHWASHER_IN_CORNER = "DISHWASHER_IN_CORNER";
	public static final String LANDING_RANGE = "LANDING_RANGE";
	public static final String LANDING_SINK = "LANDING_SINK";
	public static final String LANDING_FRIDGE = "LANDING_FRIDGE";
	public static final String WORK_TRIANGLE = "WORK_TRIANGLE";
	public static final String WORK_TRIANGLE_BLOCKED = "WORK_TRIANGLE_BLOCKED";
	public static final String UPPER_ALIGNMENT = "UPPER_ALIGNMENT";
	public static final String UPPER_TOP_LINE = "UPPER_TOP_LINE";
	public static final String RANGE_CLEARANCE = "RANGE_CLEARANCE";
	public static final String TALL_MID_RUN = "TALL_MID_RUN";
	public static final String HOOD_WITHOUT_RANGE = "HOOD_WITHOUT_RANGE";
	public static final String HOOD_OVER_RANGE = "HOOD_OVER_RANGE";

	static final double TOLERANCE_MM = 0.5;
	/**
	 * How far from a corner a run still counts as reaching it.
	 */
	static final double CORNER_REACH_MM = 700;
	/**
	 * Fillers above this elevation belong with the wall cabinets.
	 */
	static final double UPPER_ELEVATION_MM = 1000;
	/**
	 * Windows above this elevation are over the counter.
	 */
	static final double WINDOW_OVER_COUNTER_MM = 700;
	static final double MIN_CORNER_ANGLE = 10;
	static final Pattern SINK = Pattern.compile("(?i)sink|évier|evier");

	/**
	 * The lengths the guidelines use, in millimetres, for a Shop's units.
	 */
	record Measures(double widthStep, double fillerMin, double fillerMax, double cornerClearance, double endGapMax,
			double rangeLandingShort, double rangeLandingLong, double sinkLandingShort, double sinkLandingLong,
			double fridgeLanding, double triangleLegMin, double triangleLegMax, double triangleTotalMax,
			double rangeClearance, double centreTolerance, double alignTolerance) {
		static Measures of(Units units) {
			return units == Units.METRIC
					? new Measures(50, 25, 75, 25, 300, 300, 380, 450, 600, 380, 1200, 2700, 7900, 600, 25, 3)
					: new Measures(3 * IN, 1 * IN, 3 * IN, 1 * IN, 12 * IN, 12 * IN, 15 * IN, 18 * IN, 24 * IN, 15 * IN,
							48 * IN, 108 * IN, 312 * IN, 24 * IN, 1 * IN, 3);
		}
	}

	static final double IN = 25.4;

	enum Kind {
		BASE, TALL, FRIDGE, RANGE, DISHWASHER, FILLER;

		boolean counter() {
			return this == BASE || this == DISHWASHER || this == FILLER;
		}

		boolean tall() {
			return this == TALL || this == FRIDGE;
		}
	}

	/**
	 * Something standing on the floor along a Wall, or a wall cabinet, from {@code start} to
	 * {@code end} along it.
	 */
	record Element(String id, Kind kind, double start, double end, double depth, double bottom, double top,
			boolean sink, boolean blind) {
		double width() {
			return end - start;
		}

		double centre() {
			return (start + end) / 2;
		}
	}

	private DesignGuidelines() {
	}

	public static List<Remark> check(Design design, List<CabinetTemplate> templates, Units units) {
		var measures = Measures.of(units);
		var walls = list(design.getWalls());
		var templatesByKey = templates.stream()
				.collect(Collectors.toMap(CabinetTemplate::key, Function.identity(), (first, second) -> first));
		var floor = new LinkedHashMap<String, List<Element>>();
		var uppers = new LinkedHashMap<String, List<Element>>();
		walls.forEach(wall -> {
			floor.put(wall.getId(), new ArrayList<>());
			uppers.put(wall.getId(), new ArrayList<>());
		});

		for (var cabinet : list(design.getCabinets())) {
			var template = templatesByKey.get(cabinet.templateKey());
			if (template == null || !floor.containsKey(cabinet.getWallId())) {
				continue;
			}
			var element = new Element(cabinet.getId(), template.getType() == CabinetType.TALL ? Kind.TALL : Kind.BASE,
					cabinet.getOffsetMm(), cabinet.getOffsetMm() + cabinet.getWidthMm(), cabinet.getDepthMm(),
					cabinet.getElevationMm(), cabinet.getElevationMm() + cabinet.getHeightMm(), isSink(template),
					template.getBlindWidthMm() > 0);
			(template.getType() == CabinetType.WALL ? uppers : floor).get(cabinet.getWallId()).add(element);
		}
		for (var appliance : list(design.getAppliances())) {
			if (appliance.getKind() == ApplianceKind.HOOD || !floor.containsKey(appliance.getWallId())) {
				continue;
			}
			var kind = switch (appliance.getKind()) {
			case FRIDGE -> Kind.FRIDGE;
			case RANGE -> Kind.RANGE;
			default -> Kind.DISHWASHER;
			};
			floor.get(appliance.getWallId()).add(new Element(appliance.getId(), kind, appliance.getOffsetMm(),
					appliance.getOffsetMm() + appliance.getWidthMm(), appliance.getDepthMm(), appliance.getElevationMm(),
					appliance.getElevationMm() + appliance.getHeightMm(), false, false));
		}
		for (var filler : list(design.getFillers())) {
			if (!floor.containsKey(filler.getWallId())) {
				continue;
			}
			(filler.getElevationMm() >= UPPER_ELEVATION_MM ? uppers : floor).get(filler.getWallId())
					.add(new Element(filler.getId(), Kind.FILLER, filler.getOffsetMm(), filler.getOffsetMm() + filler.getWidthMm(),
							filler.getDepthMm(), filler.getElevationMm(), filler.getElevationMm() + filler.getHeightMm(), false,
							false));
		}
		floor.values().forEach(elements -> elements.sort(Comparator.comparingDouble(Element::start)));
		uppers.values().forEach(elements -> elements.sort(Comparator.comparingDouble(Element::start)));

		var remarks = new ArrayList<Remark>();
		for (var index = 0; index < walls.size(); index++) {
			var wall = walls.get(index);
			var previous = index > 0 && corner(walls.get(index - 1)) ? walls.get(index - 1) : null;
			var next = index + 1 < walls.size() && corner(wall) ? walls.get(index + 1) : null;
			var run = floor.get(wall.getId());
			var start = previous == null ? 0 : cornerDepth(floor.get(previous.getId()), previous, true);
			var end = wall.getLengthMm() - (next == null ? 0 : cornerDepth(floor.get(next.getId()), next, false));

			gaps(wall, run, start, end, design, measures, remarks);
			fillers(wall, run, start, end, measures, remarks);
			sinks(wall, run, design, measures, remarks);
			dishwashers(wall, run, start, end, start > 0, end < wall.getLengthMm(), measures, remarks);
			landing(wall, run, measures, remarks);
			tall(wall, run, measures, remarks);
			upperRun(wall, uppers.get(wall.getId()), run, measures, remarks);
			hoods(wall, run, design, measures, remarks);
			if (next != null) {
				corner(wall, run, next, floor.get(next.getId()), measures, remarks, false);
				corner(wall, uppers.get(wall.getId()), next, uppers.get(next.getId()), measures, remarks, true);
			}
		}
		widths(design, templatesByKey, measures, remarks);
		triangle(walls, floor, measures, remarks);
		return remarks;
	}

	/**
	 * Gaps in a run that nothing explains: between two things on the floor (unless a doorway is
	 * there), or a sliver left at a run's end, too narrow to be useful.
	 */
	private static void gaps(Wall wall, List<Element> run, double start, double end, Design design, Measures measures,
			List<Remark> remarks) {
		for (var index = 1; index < run.size(); index++) {
			var before = run.get(index - 1);
			var after = run.get(index);
			var gap = after.start() - before.end();
			if (gap > TOLERANCE_MM && !doorway(design, wall, before.end(), after.start())) {
				remarks.add(remark(GAP, wall, gap, measures.fillerMax(), before.id(), after.id()));
			}
		}
		if (!run.isEmpty()) {
			var first = run.get(0);
			var atStart = first.start() - start;
			if (atStart > TOLERANCE_MM && atStart < measures.endGapMax()) {
				remarks.add(remark(GAP_AT_END, wall, atStart, measures.fillerMax(), first.id()));
			}
			var last = run.get(run.size() - 1);
			var atEnd = end - last.end();
			if (atEnd > TOLERANCE_MM && atEnd < measures.endGapMax()) {
				remarks.add(remark(GAP_AT_END, wall, atEnd, measures.fillerMax(), last.id()));
			}
		}
	}

	/**
	 * Fillers belong at a run's ends (against a wall or corner, or beside a tall unit or fridge), as
	 * wide as a filler can be.
	 */
	private static void fillers(Wall wall, List<Element> run, double start, double end, Measures measures,
			List<Remark> remarks) {
		for (var index = 0; index < run.size(); index++) {
			var filler = run.get(index);
			if (filler.kind() != Kind.FILLER) {
				continue;
			}
			if (filler.width() < measures.fillerMin() - TOLERANCE_MM || filler.width() > measures.fillerMax() + TOLERANCE_MM) {
				remarks.add(Remark.builder().code(FILLER_WIDTH).wallId(wall.getId()).ids(List.of(filler.id()))
						.measureMm(tenth(filler.width())).limitMm(filler.width() < measures.fillerMin() ? measures.fillerMin() : measures.fillerMax())
						.build());
			}
			var left = index > 0 && touching(run.get(index - 1), filler) ? run.get(index - 1) : null;
			var right = index + 1 < run.size() && touching(filler, run.get(index + 1)) ? run.get(index + 1) : null;
			var atEnd = filler.start() <= start + TOLERANCE_MM || filler.end() >= end - TOLERANCE_MM;
			if (!atEnd && left != null && right != null && !left.kind().tall() && !right.kind().tall()) {
				remarks.add(remark(FILLER_MID_RUN, wall, null, null, filler.id()));
			}
		}
	}

	/**
	 * A sink base on a wall with a window over the counter is centred under it.
	 */
	private static void sinks(Wall wall, List<Element> run, Design design, Measures measures, List<Remark> remarks) {
		var windows = list(design.getOpenings()).stream()
				.filter(opening -> opening.getKind() == OpeningKind.WINDOW && opening.getWallId().equals(wall.getId())
						&& opening.getElevationMm() >= WINDOW_OVER_COUNTER_MM)
				.toList();
		if (windows.isEmpty()) {
			return;
		}
		for (var sink : run.stream().filter(Element::sink).toList()) {
			var nearest = windows.stream()
					.min(Comparator.comparingDouble(window -> Math.abs(window.getOffsetMm() + window.getWidthMm() / 2 - sink.centre())))
					.orElseThrow();
			var off = Math.abs(nearest.getOffsetMm() + nearest.getWidthMm() / 2 - sink.centre());
			if (off > measures.centreTolerance() + TOLERANCE_MM) {
				remarks.add(remark(SINK_NOT_CENTRED, wall, off, measures.centreTolerance(), sink.id(), nearest.getId()));
			}
		}
	}

	/**
	 * A dishwasher stands beside the sink base, a filler at most between them, and never in a
	 * corner, against the run on the other wall.
	 */
	private static void dishwashers(Wall wall, List<Element> run, double start, double end, boolean cornerAtStart,
			boolean cornerAtEnd, Measures measures, List<Remark> remarks) {
		for (var index = 0; index < run.size(); index++) {
			var dishwasher = run.get(index);
			if (dishwasher.kind() != Kind.DISHWASHER) {
				continue;
			}
			if (!besideSink(run, index, -1, measures) && !besideSink(run, index, 1, measures)) {
				remarks.add(remark(DISHWASHER_NOT_BESIDE_SINK, wall, null, null, dishwasher.id()));
			}
			if (cornerAtStart && dishwasher.start() <= start + measures.fillerMax() + TOLERANCE_MM
					|| cornerAtEnd && dishwasher.end() >= end - measures.fillerMax() - TOLERANCE_MM) {
				remarks.add(remark(DISHWASHER_IN_CORNER, wall, null, null, dishwasher.id()));
			}
		}
	}

	private static boolean besideSink(List<Element> run, int index, int direction, Measures measures) {
		var current = run.get(index);
		var between = 0.0;
		for (var at = index + direction; at >= 0 && at < run.size(); at += direction) {
			var other = run.get(at);
			var gap = direction > 0 ? other.start() - current.end() : current.start() - other.end();
			if (gap > TOLERANCE_MM) {
				return false;
			}
			if (other.sink()) {
				return between <= measures.fillerMax() + TOLERANCE_MM;
			}
			if (other.kind() != Kind.FILLER) {
				return false;
			}
			between += other.width();
			current = other;
		}
		return false;
	}

	/**
	 * Counter to set things down beside the range, the sink and the fridge.
	 */
	private static void landing(Wall wall, List<Element> run, Measures measures, List<Remark> remarks) {
		for (var index = 0; index < run.size(); index++) {
			var element = run.get(index);
			var left = counter(run, index, -1);
			var right = counter(run, index, 1);
			var shorter = Math.min(left, right);
			var longer = Math.max(left, right);
			if (element.kind() == Kind.RANGE
					&& (shorter < measures.rangeLandingShort() - TOLERANCE_MM || longer < measures.rangeLandingLong() - TOLERANCE_MM)) {
				remarks.add(Remark.builder().code(LANDING_RANGE).wallId(wall.getId()).ids(List.of(element.id()))
						.measureMm(tenth(shorter)).limitMm(measures.rangeLandingShort()).build());
			}
			if (element.sink()
					&& (shorter < measures.sinkLandingShort() - TOLERANCE_MM || longer < measures.sinkLandingLong() - TOLERANCE_MM)) {
				remarks.add(Remark.builder().code(LANDING_SINK).wallId(wall.getId()).ids(List.of(element.id()))
						.measureMm(tenth(shorter)).limitMm(measures.sinkLandingShort()).build());
			}
			if (element.kind() == Kind.FRIDGE && longer < measures.fridgeLanding() - TOLERANCE_MM) {
				remarks.add(Remark.builder().code(LANDING_FRIDGE).wallId(wall.getId()).ids(List.of(element.id()))
						.measureMm(tenth(longer)).limitMm(measures.fridgeLanding()).build());
			}
		}
	}

	/**
	 * The counter running from an element in one direction, up to a gap, a tall unit, the range or
	 * the run's end.
	 */
	private static double counter(List<Element> run, int index, int direction) {
		var current = run.get(index);
		var length = 0.0;
		for (var at = index + direction; at >= 0 && at < run.size(); at += direction) {
			var other = run.get(at);
			var gap = direction > 0 ? other.start() - current.end() : current.start() - other.end();
			if (gap > TOLERANCE_MM || !other.kind().counter()) {
				break;
			}
			length += other.width();
			current = other;
		}
		return length;
	}

	/**
	 * A tall unit or fridge ends a run; between two counters it cuts the counter in two.
	 */
	private static void tall(Wall wall, List<Element> run, Measures measures, List<Remark> remarks) {
		for (var index = 0; index < run.size(); index++) {
			var element = run.get(index);
			if (element.kind().tall() && counterBeside(run, index, -1, measures) && counterBeside(run, index, 1, measures)) {
				remarks.add(remark(TALL_MID_RUN, wall, null, null, element.id()));
			}
		}
	}

	private static boolean counterBeside(List<Element> run, int index, int direction, Measures measures) {
		var current = run.get(index);
		for (var at = index + direction; at >= 0 && at < run.size(); at += direction) {
			var other = run.get(at);
			var gap = direction > 0 ? other.start() - current.end() : current.start() - other.end();
			if (gap > TOLERANCE_MM) {
				return false;
			}
			if (other.kind() != Kind.FILLER) {
				return other.kind().counter() || other.kind() == Kind.RANGE;
			}
			current = other;
		}
		return false;
	}

	/**
	 * Wall cabinets line up with what stands below them, share one top line, and leave room over
	 * the range.
	 */
	private static void upperRun(Wall wall, List<Element> uppers, List<Element> run, Measures measures, List<Remark> remarks) {
		var cabinets = uppers.stream().filter(element -> element.kind() != Kind.FILLER).toList();
		for (var upper : cabinets) {
			for (var edge : List.of(upper.start(), upper.end())) {
				var inside = run.stream()
						.filter(below -> below.start() + measures.alignTolerance() < edge && edge < below.end() - measures.alignTolerance())
						.findFirst();
				if (inside.isPresent()) {
					remarks.add(remark(UPPER_ALIGNMENT, wall, null, null, upper.id(), inside.get().id()));
					break;
				}
			}
			run.stream()
					.filter(below -> below.kind() == Kind.RANGE && overlap(below, upper) > TOLERANCE_MM)
					.filter(range -> upper.bottom() - range.top() < measures.rangeClearance() - TOLERANCE_MM)
					.findFirst()
					.ifPresent(range -> remarks.add(remark(RANGE_CLEARANCE, wall, upper.bottom() - range.top(),
							measures.rangeClearance(), upper.id(), range.id())));
		}
		if (cabinets.size() > 1) {
			var tops = cabinets.stream()
					.collect(Collectors.groupingBy(upper -> Math.round(upper.top()), Collectors.counting()));
			var common = tops.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
			cabinets.stream()
					.filter(upper -> Math.abs(upper.top() - common) > measures.alignTolerance())
					.forEach(upper -> remarks.add(remark(UPPER_TOP_LINE, wall, upper.top() - common, null, upper.id())));
		}
	}

	/**
	 * A hood is centred over a range on its wall, at least as wide.
	 */
	private static void hoods(Wall wall, List<Element> run, Design design, Measures measures, List<Remark> remarks) {
		for (var hood : list(design.getAppliances())) {
			if (hood.getKind() != ApplianceKind.HOOD || !hood.getWallId().equals(wall.getId())) {
				continue;
			}
			var centre = hood.getOffsetMm() + hood.getWidthMm() / 2;
			var range = run.stream()
					.filter(element -> element.kind() == Kind.RANGE
							&& Math.min(element.end(), hood.getOffsetMm() + hood.getWidthMm()) - Math.max(element.start(), hood.getOffsetMm()) > TOLERANCE_MM)
					.findFirst();
			if (range.isEmpty()) {
				remarks.add(remark(HOOD_WITHOUT_RANGE, wall, null, null, hood.getId()));
			} else if (Math.abs(range.get().centre() - centre) > measures.centreTolerance() + TOLERANCE_MM
					|| hood.getWidthMm() < range.get().width() - TOLERANCE_MM) {
				remarks.add(remark(HOOD_OVER_RANGE, wall, null, null, hood.getId(), range.get().id()));
			}
		}
	}

	/**
	 * Where two runs meet in a corner: base cabinets turn it with a blind corner cabinet, and the run
	 * on the other wall keeps clear of it by a filler's width.
	 */
	private static void corner(Wall wall, List<Element> before, Wall next, List<Element> after, Measures measures,
			List<Remark> remarks, boolean upper) {
		var reachingEnd = before.stream().filter(element -> element.end() >= wall.getLengthMm() - CORNER_REACH_MM).toList();
		var reachingStart = after.stream().filter(element -> element.start() <= CORNER_REACH_MM).toList();
		if (reachingEnd.isEmpty() || reachingStart.isEmpty()) {
			return;
		}
		var intoCorner = before.stream().filter(element -> element.end() >= wall.getLengthMm() - TOLERANCE_MM).findFirst();
		var fromCorner = after.stream().filter(element -> element.start() <= TOLERANCE_MM).findFirst();
		if (!upper) {
			var corner = intoCorner.or(() -> fromCorner);
			if (corner.isEmpty() || !corner.get().blind()) {
				var ids = new ArrayList<String>();
				corner.ifPresent(element -> ids.add(element.id()));
				if (corner.isEmpty()) {
					ids.add(reachingEnd.get(reachingEnd.size() - 1).id());
					ids.add(reachingStart.get(0).id());
				}
				remarks.add(Remark.builder().code(CORNER_NOT_BLIND).wallId(next.getId()).ids(List.copyOf(ids)).build());
			}
		}
		var code = upper ? UPPER_CORNER_CLEARANCE : CORNER_CLEARANCE;
		if (intoCorner.isPresent()) {
			clearance(after, intoCorner.get().depth(), true, next, code, intoCorner.get(), measures, remarks);
		} else if (fromCorner.isPresent()) {
			clearance(before, wall.getLengthMm() - fromCorner.get().depth(), false, wall, code, fromCorner.get(), measures, remarks);
		}
	}

	/**
	 * The first thing other than a filler on the other wall stands at least a filler's width clear
	 * of the cabinet in the corner.
	 */
	private static void clearance(List<Element> other, double cornerFace, boolean fromStart, Wall otherWall, String code,
			Element corner, Measures measures, List<Remark> remarks) {
		var ordered = fromStart ? other : other.reversed();
		ordered.stream()
				.filter(element -> element.kind() != Kind.FILLER)
				.findFirst()
				.ifPresent(first -> {
					var clear = fromStart ? first.start() - cornerFace : cornerFace - first.end();
					if (clear < measures.cornerClearance() - TOLERANCE_MM && clear < CORNER_REACH_MM) {
						remarks.add(remark(code, otherWall, clear, measures.cornerClearance(), first.id(), corner.id()));
					}
				});
	}

	/**
	 * Cabinet widths in the Shop's steps: 3" for imperial Shops, 50 mm for metric ones.
	 */
	private static void widths(Design design, Map<String, CabinetTemplate> templates, Measures measures, List<Remark> remarks) {
		for (var cabinet : list(design.getCabinets())) {
			if (!templates.containsKey(cabinet.templateKey())) {
				continue;
			}
			var steps = cabinet.getWidthMm() / measures.widthStep();
			if (Math.abs(steps - Math.round(steps)) * measures.widthStep() > TOLERANCE_MM) {
				remarks.add(Remark.builder().code(WIDTH_STEP).wallId(cabinet.getWallId()).ids(List.of(cabinet.getId()))
						.measureMm(cabinet.getWidthMm()).limitMm(measures.widthStep()).build());
			}
		}
	}

	/**
	 * Sink, range and fridge each 4'–9' apart, 26' in all, nothing tall standing across a leg; only
	 * when there is one of each.
	 */
	private static void triangle(List<Wall> walls, Map<String, List<Element>> floor, Measures measures, List<Remark> remarks) {
		var placements = RoomLayout.layout(walls);
		var points = new HashMap<String, RoomLayout.Point>();
		var found = new LinkedHashMap<String, Element>();
		var tall = new ArrayList<List<RoomLayout.Point>>();
		var tallIds = new ArrayList<String>();
		var counts = new HashMap<String, Integer>();
		floor.forEach((wallId, run) -> {
			var placement = placements.get(wallId);
			for (var element : run) {
				var role = element.sink() ? "sink" : element.kind() == Kind.RANGE ? "range" : element.kind() == Kind.FRIDGE ? "fridge" : null;
				if (role != null) {
					counts.merge(role, 1, Integer::sum);
					found.put(role, element);
					points.put(role, placement.at(element.centre(), element.depth() / 2));
				}
				if (element.kind() == Kind.TALL) {
					tall.add(placement.rectangle(element.start(), element.end(), 0, element.depth()));
					tallIds.add(element.id());
				}
			}
		});
		if (counts.size() < 3 || counts.values().stream().anyMatch(count -> count != 1)) {
			return;
		}
		var legs = List.of(List.of("sink", "range"), List.of("range", "fridge"), List.of("fridge", "sink"));
		var total = 0.0;
		var ids = List.of(found.get("sink").id(), found.get("range").id(), found.get("fridge").id());
		for (var leg : legs) {
			var from = points.get(leg.get(0));
			var to = points.get(leg.get(1));
			var length = Math.hypot(to.x() - from.x(), to.z() - from.z());
			total += length;
			if (length < measures.triangleLegMin() - TOLERANCE_MM || length > measures.triangleLegMax() + TOLERANCE_MM) {
				remarks.add(Remark.builder().code(WORK_TRIANGLE).ids(List.of(found.get(leg.get(0)).id(), found.get(leg.get(1)).id()))
						.measureMm(tenth(length)).limitMm(length < measures.triangleLegMin() ? measures.triangleLegMin() : measures.triangleLegMax())
						.build());
			}
			for (var index = 0; index < tall.size(); index++) {
				if (crosses(from, to, tall.get(index))) {
					remarks.add(Remark.builder().code(WORK_TRIANGLE_BLOCKED)
							.ids(List.of(tallIds.get(index), found.get(leg.get(0)).id(), found.get(leg.get(1)).id())).build());
				}
			}
		}
		if (total > measures.triangleTotalMax() + TOLERANCE_MM) {
			remarks.add(Remark.builder().code(WORK_TRIANGLE).ids(ids).measureMm(tenth(total)).limitMm(measures.triangleTotalMax()).build());
		}
	}

	/**
	 * Whether a segment crosses a convex outline, or starts or ends inside it.
	 */
	static boolean crosses(RoomLayout.Point from, RoomLayout.Point to, List<RoomLayout.Point> outline) {
		for (var index = 0; index < outline.size(); index++) {
			if (intersect(from, to, outline.get(index), outline.get((index + 1) % outline.size()))) {
				return true;
			}
		}
		return false;
	}

	private static boolean intersect(RoomLayout.Point a, RoomLayout.Point b, RoomLayout.Point c, RoomLayout.Point d) {
		var d1 = cross(c, d, a);
		var d2 = cross(c, d, b);
		var d3 = cross(a, b, c);
		var d4 = cross(a, b, d);
		return (d1 > 0) != (d2 > 0) && (d3 > 0) != (d4 > 0);
	}

	private static double cross(RoomLayout.Point origin, RoomLayout.Point to, RoomLayout.Point point) {
		return (to.x() - origin.x()) * (point.z() - origin.z()) - (to.z() - origin.z()) * (point.x() - origin.x());
	}

	private static boolean isSink(CabinetTemplate template) {
		return "BASE_SINK".equals(template.getCode())
				|| Optional.ofNullable(template.getName()).map(name -> SINK.matcher(name).find()).orElse(false);
	}

	/**
	 * Whether a Wall turns into the next one: a corner, rather than a straight continuation.
	 */
	private static boolean corner(Wall wall) {
		return Math.abs(wall.getAngleToNextDegrees()) > MIN_CORNER_ANGLE;
	}

	/**
	 * How deep the run on a neighbouring Wall stands in the shared corner: what this Wall's run
	 * starts or ends against.
	 */
	private static double cornerDepth(List<Element> run, Wall neighbour, boolean atItsEnd) {
		return run.stream()
				.filter(element -> atItsEnd ? element.end() >= neighbour.getLengthMm() - TOLERANCE_MM : element.start() <= TOLERANCE_MM)
				.mapToDouble(Element::depth)
				.max()
				.orElse(0);
	}

	private static boolean doorway(Design design, Wall wall, double from, double to) {
		return list(design.getOpenings()).stream()
				.anyMatch(opening -> opening.getKind() == OpeningKind.DOORWAY && opening.getWallId().equals(wall.getId())
						&& opening.getOffsetMm() < to && opening.getOffsetMm() + opening.getWidthMm() > from);
	}

	private static boolean touching(Element left, Element right) {
		return Math.abs(right.start() - left.end()) <= TOLERANCE_MM;
	}

	private static double overlap(Element first, Element second) {
		return Math.min(first.end(), second.end()) - Math.max(first.start(), second.start());
	}

	private static Remark remark(String code, Wall wall, Double measure, Double limit, String... ids) {
		return Remark.builder().code(code).wallId(wall.getId()).ids(List.of(ids)).measureMm(tenth(measure)).limitMm(tenth(limit)).build();
	}

	/**
	 * A length to the tenth of a millimetre, as Members read it.
	 */
	private static Double tenth(Double millimetres) {
		return millimetres == null ? null : Math.round(millimetres * 10) / 10.0;
	}

	private static <T> List<T> list(List<T> items) {
		return items == null ? List.of() : items;
	}
}
