package dev.daniellavoie.opencabinet.project.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.FrontKind;
import dev.daniellavoie.opencabinet.project.domain.model.FrontRow;

/**
 * Sizes a Cabinet's front rows. Shared by part generation and, as {@code front-layout.ts}, the 3D
 * view, so both draw the same fronts.
 */
public final class FrontLayout {
	/**
	 * Fronts wider than this get two door leaves.
	 */
	public static final double DOUBLE_DOOR_MIN_WIDTH_MM = 609.6;

	/**
	 * A sized row: {@code bottomMm} is measured from the bottom of the box, {@code heightMm} is the
	 * front's own height (gaps already taken off).
	 */
	public record Row(FrontKind kind, double bottomMm, double heightMm) {
	}

	private FrontLayout() {
	}

	/**
	 * Rows from the top, each followed by one gap. Fixed rows keep their height; flexible rows
	 * share what remains by weight. Rows left without height are dropped.
	 */
	public static List<Row> rows(CabinetTemplate template, double boxHeightMm, double gapMm) {
		var rows = Optional.ofNullable(template.getFrontRows()).orElse(List.of());
		var available = boxHeightMm - gapMm * rows.size();
		var fixed = rows.stream().filter(row -> row.getHeightMm() != null).mapToDouble(FrontRow::getHeightMm).sum();
		var weights = rows.stream().filter(row -> row.getHeightMm() == null).mapToDouble(FrontRow::effectiveWeight)
				.sum();
		var flexible = Math.max(0, available - fixed);

		var sized = new ArrayList<Row>();
		var top = boxHeightMm;
		for (var row : rows) {
			var height = row.getHeightMm() != null ? row.getHeightMm() : flexible * row.effectiveWeight() / weights;
			top -= gapMm / 2;
			if (height > 0) {
				sized.add(new Row(row.getKind(), top - height, height));
			}
			top -= height + gapMm / 2;
		}
		return sized;
	}

	/**
	 * The width the fronts cover, after the blind portion.
	 */
	public static double frontWidth(CabinetTemplate template, double cabinetWidthMm) {
		return cabinetWidthMm - template.getBlindWidthMm();
	}

	public static int doorLeaves(double frontWidthMm) {
		return frontWidthMm > DOUBLE_DOOR_MIN_WIDTH_MM ? 2 : 1;
	}
}
