package dev.daniellavoie.opencabinet.project.domain.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


/**
 * What a Cabinet Template is made of when it is an assembly (docs/adr/0006-template-is-an-assembly-of-parts.md):
 * Parts and front stacks held by Attachments, a divider Part placed per Cabinet, and hardware.
 * Everything is measured in the Cabinet's frame: x from its left side, y up from its bottom, z
 * from its back towards its front.
 */
public record TemplateAssembly(List<AssemblyPart> parts, List<FrontStack> frontStacks, AssemblyPart divider,
		List<Hardware> hardware) {

	public List<AssemblyPart> partsOrEmpty() {
		return Optional.ofNullable(parts).orElse(List.of());
	}

	public List<FrontStack> frontStacksOrEmpty() {
		return Optional.ofNullable(frontStacks).orElse(List.of());
	}

	public List<Hardware> hardwareOrEmpty() {
		return Optional.ofNullable(hardware).orElse(List.of());
	}

	public enum Axis {
		X, Y, Z
	}

	/**
	 * A box's faces: LEFT and RIGHT across x, BOTTOM and TOP across y, BACK and FRONT across z.
	 */
	public enum Face {
		LEFT(Axis.X, false), RIGHT(Axis.X, true), BOTTOM(Axis.Y, false), TOP(Axis.Y, true), BACK(Axis.Z, false),
		FRONT(Axis.Z, true);

		private final Axis axis;
		private final boolean max;

		Face(Axis axis, boolean max) {
			this.axis = axis;
			this.max = max;
		}

		public Axis axis() {
			return axis;
		}

		/**
		 * Whether the face is at the high end of its axis (RIGHT, TOP, FRONT).
		 */
		public boolean max() {
			return max;
		}

		public static Face of(Axis axis, boolean max) {
			for (var face : values()) {
				if (face.axis == axis && face.max == max) {
					return face;
				}
			}
			throw new IllegalArgumentException(axis.name());
		}
	}

	public enum PartKind {
		/**
		 * Cut from a sheet.
		 */
		PANEL,
		/**
		 * Bought by the metre, along the Part's longest side.
		 */
		MOULDING
	}

	/**
	 * Lengths a template can follow from the Construction Standard.
	 */
	public enum StandardValue {
		DOOR_GAP, TOE_KICK_HEIGHT
	}

	/**
	 * Materials a template can take from the Construction Standard, so it follows the Shop's choices.
	 */
	public enum MaterialRole {
		CARCASS, BACK, FRONT, CARCASS_BANDING, FRONT_BANDING, CROWN_MOULDING, HINGE, SHELF_PIN, LEG, PULL,
		DRAWER_SLIDE, DRAWER_BOX
	}

	/**
	 * Per-Cabinet counts a repeated Part can follow.
	 */
	public enum Count {
		SHELVES
	}

	/**
	 * A Construction Standard role, or a specific catalog material.
	 */
	public record MaterialRef(MaterialRole role, UUID materialId) {
	}

	/**
	 * One term of an offset: millimetres, a Construction Standard value or a material's thickness,
	 * times {@code factor} (1 when unset).
	 */
	public record Term(Double mm, StandardValue standard, MaterialRef thickness, Double factor) {
		public double effectiveFactor() {
			return factor == null ? 1 : factor;
		}
	}

	/**
	 * A face held at {@code face} of the Cabinet ({@code to} = CABINET) or of the Part or front stack
	 * named {@code to}, moved along the axis by the sum of {@code offset}.
	 */
	public record Attachment(String to, Face face, List<Term> offset) {
		public static final String CABINET = "CABINET";

		public List<Term> offsetOrEmpty() {
			return Optional.ofNullable(offset).orElse(List.of());
		}
	}

	/**
	 * Which faces of a panel are edge banded, and with what.
	 */
	public record Banding(List<Face> faces, MaterialRef material) {
	}

	/**
	 * A Part's copies, spaced evenly between its two faces on its thickness axis, kept
	 * {@code dividerGapMm} clear of each divider.
	 */
	public record Repeat(Count count, Double dividerGapMm) {
		public double dividerGap() {
			return dividerGapMm == null ? 0 : dividerGapMm;
		}
	}

	/**
	 * A box. On its thickness axis one face is attached and its size is the material's thickness;
	 * on the other axes two of its two faces and its {@code sizes} are given. A repeated Part
	 * attaches both faces of its thickness axis: the span its copies share. {@code front} marks a Part
	 * that is a Front outside any front stack, such as a blind corner's panel: it is ordered with the
	 * Cabinet's Fronts.
	 */
	public record AssemblyPart(String id, PartRole role, String name, PartKind kind, MaterialRef material,
			Axis thickness, Axis grain, Map<Face, Attachment> faces, Map<Axis, List<Term>> sizes, Repeat repeat,
			Banding banding, Boolean front) {
		public PartKind kindOrPanel() {
			return kind == null ? PartKind.PANEL : kind;
		}

		public boolean isFront() {
			return Boolean.TRUE.equals(front);
		}
	}

	/**
	 * One row of a front stack, from the top: a fixed height or a share of what is left.
	 */
	public record StackRow(FrontKind kind, Double heightMm, Double weight) {
		public FrontRow frontRow() {
			return FrontRow.builder().kind(kind).heightMm(heightMm).weight(weight == null ? 0 : weight).build();
		}
	}

	/**
	 * Fronts laid out in rows, like a starter template's front rows, inside the area its LEFT,
	 * RIGHT, BOTTOM and TOP faces are attached to, with their BACK face attached. Door rows get two
	 * leaves when wider than {@code splitAboveMm}.
	 */
	public record FrontStack(String id, MaterialRef material, Map<Face, Attachment> faces, List<StackRow> rows,
			Double splitAboveMm) {
	}

	/**
	 * How many of a hardware item per host: {@code fixed}; one per {@code perMm} along the host's
	 * {@code axis}, at least {@code minimum}; or by {@code steps} of the host's size along
	 * {@code axis} (the Cabinet's when there is no host).
	 */
	public record Quantity(Integer fixed, Double perMm, Axis axis, Integer minimum, List<Step> steps) {
	}

	/**
	 * {@code quantity} for sizes up to {@code upToMm}; the last step, without a limit, for anything
	 * larger.
	 */
	public record Step(Double upToMm, int quantity) {
	}

	/**
	 * Hardware counted for each Part (or each copy) named {@code on}, or each Front of the front stack
	 * named {@code on} (only of {@code frontKind} when given), or once per Cabinet when {@code on} is
	 * unset.
	 */
	public record Hardware(MaterialRef material, String on, FrontKind frontKind, Quantity quantity) {
	}
}
