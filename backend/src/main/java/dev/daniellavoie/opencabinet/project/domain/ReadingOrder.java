package dev.daniellavoie.opencabinet.project.domain;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.Design;

/**
 * The order Cabinets are read and labelled in (C1, C2, …) on every output: by Wall, then from the
 * Wall's start, then from the floor up. The editor labels them the same way.
 */
public final class ReadingOrder {
	private ReadingOrder() {
	}

	public static List<Cabinet> cabinets(Design design) {
		var walls = Optional.ofNullable(design.getWalls()).orElse(List.of());
		var wallIndex = IntStream.range(0, walls.size())
				.boxed()
				.collect(Collectors.toMap(index -> walls.get(index).getId(), Function.identity(),
						(first, second) -> first));

		return Optional.ofNullable(design.getCabinets())
				.orElse(List.of())
				.stream()
				.sorted(Comparator.<Cabinet>comparingInt(cabinet -> wallIndex.getOrDefault(cabinet.getWallId(),
						Integer.MAX_VALUE))
						.thenComparingDouble(Cabinet::getOffsetMm)
						.thenComparingDouble(Cabinet::getElevationMm))
				.toList();
	}

	/**
	 * Cabinet id to label, in reading order.
	 */
	public static Map<String, String> labels(Design design) {
		var cabinets = cabinets(design);
		var labels = new LinkedHashMap<String, String>();
		IntStream.range(0, cabinets.size()).forEach(index -> labels.put(cabinets.get(index).getId(), "C" + (index + 1)));
		return labels;
	}
}
