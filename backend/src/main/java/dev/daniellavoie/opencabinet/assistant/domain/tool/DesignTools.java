package dev.daniellavoie.opencabinet.assistant.domain.tool;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import tools.jackson.databind.JsonNode;

import dev.daniellavoie.opencabinet.project.domain.DesignEdits;
import dev.daniellavoie.opencabinet.project.domain.DesignEdits.ItemChanges;
import dev.daniellavoie.opencabinet.project.domain.model.ApplianceKind;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.FrontSource;
import dev.daniellavoie.opencabinet.project.domain.model.OpeningKind;

/**
 * The Design Assistant's tools: one or more for every editor action. Lengths are millimetres.
 */
public final class DesignTools {
	private static final String WALL_SIZE = """
			"lengthMm": {"type": "number", "description": "Length along the floor, mm"},
			"heightMm": {"type": "number", "description": "Ceiling height, mm"},
			"angleToNextDegrees": {"type": "number", "description": "Counter-clockwise turn to the next wall, seen from above"}""";

	private static final String ITEM_PLACE = """
			"offsetMm": {"type": "number", "description": "From the wall's start to the item's left side, mm"},
			"widthMm": {"type": "number"},
			"heightMm": {"type": "number"},
			"elevationMm": {"type": "number", "description": "Height of the item's bottom above the floor, mm"}""";

	private static final String CABINET_SIZE = """
			"offsetMm": {"type": "number", "description": "From the wall's start to the cabinet's left side, mm"},
			"widthMm": {"type": "number"},
			"heightMm": {"type": "number"},
			"depthMm": {"type": "number"},
			"elevationMm": {"type": "number", "description": "Height of the cabinet's bottom above the floor, mm; 0 for base and tall cabinets"},
			"shelfCount": {"type": "integer", "description": "Shelves in each section"},
			"mirrored": {"type": "boolean", "description": "Built left for right: a blind corner then has its blind side on the left"}""";

	public static final List<DesignTool> ALL = List.of(
			tool("add_wall", "Adds a wall at the end of the room, continuing from the last wall.",
					schema(WALL_SIZE), Set.of("addWall"),
					(design, args, context) -> {
						var id = context.newId().apply("wall");
						var added = DesignEdits.addWall(design, id);
						return new DesignTool.Outcome(DesignEdits.updateWall(added, id, wall -> wall.toBuilder()
								.lengthMm(number(args, "lengthMm").orElse(wall.getLengthMm()))
								.heightMm(number(args, "heightMm").orElse(wall.getHeightMm()))
								.angleToNextDegrees(number(args, "angleToNextDegrees").orElse(wall.getAngleToNextDegrees()))
								.build()), id);
					}),
			tool("update_wall", "Changes a wall's size or its turn to the next wall.",
					schema("\"wallId\": {\"type\": \"string\"},\n" + WALL_SIZE, "wallId"), Set.of("updateWall"),
					(design, args, context) -> new DesignTool.Outcome(DesignEdits.updateWall(design, wall(design, args), wall -> wall.toBuilder()
							.lengthMm(number(args, "lengthMm").orElse(wall.getLengthMm()))
							.heightMm(number(args, "heightMm").orElse(wall.getHeightMm()))
							.angleToNextDegrees(number(args, "angleToNextDegrees").orElse(wall.getAngleToNextDegrees()))
							.build()), null)),
			tool("remove_wall", "Removes a wall and everything on it. A room keeps at least one wall.",
					schema("\"wallId\": {\"type\": \"string\"}", "wallId"), Set.of("removeWall"),
					(design, args, context) -> new DesignTool.Outcome(DesignEdits.removeWall(design, wall(design, args)), null)),
			tool("add_opening", "Adds a window or doorway to a wall; cabinets must not cover it.",
					schema("\"kind\": {\"type\": \"string\", \"enum\": [\"WINDOW\", \"DOORWAY\"]},\n\"wallId\": {\"type\": \"string\"},\n" + ITEM_PLACE,
							"kind", "wallId"),
					Set.of("addOpening"),
					(design, args, context) -> {
						var id = context.newId().apply("opening");
						var added = DesignEdits.addOpening(design, choice(args, "kind", OpeningKind.class), wall(design, args), id);
						return new DesignTool.Outcome(DesignEdits.updateItem(added, id, itemChanges(args, false)), id);
					}),
			tool("add_appliance", "Adds an appliance placeholder (fridge, range, dishwasher, hood) that cabinets are fitted around.",
					schema("\"kind\": {\"type\": \"string\", \"enum\": [\"FRIDGE\", \"RANGE\", \"DISHWASHER\", \"HOOD\"]},\n\"wallId\": {\"type\": \"string\"},\n"
							+ ITEM_PLACE + ",\n\"depthMm\": {\"type\": \"number\"}", "kind", "wallId"),
					Set.of("addAppliance"),
					(design, args, context) -> {
						var id = context.newId().apply("appliance");
						var added = DesignEdits.addAppliance(design, choice(args, "kind", ApplianceKind.class), wall(design, args), id);
						return new DesignTool.Outcome(DesignEdits.updateItem(added, id, itemChanges(args, false)), id);
					}),
			tool("add_filler", "Adds a filler strip closing a gap beside a cabinet; base height by default.",
					schema("\"wallId\": {\"type\": \"string\"},\n" + ITEM_PLACE + ",\n\"depthMm\": {\"type\": \"number\"}", "wallId"),
					Set.of("addFiller"),
					(design, args, context) -> {
						var id = context.newId().apply("filler");
						var added = DesignEdits.addFiller(design, wall(design, args), id);
						return new DesignTool.Outcome(DesignEdits.updateItem(added, id, itemChanges(args, false)), id);
					}),
			tool("update_item", "Moves or resizes a window, doorway, appliance or filler.",
					schema("\"itemId\": {\"type\": \"string\"},\n\"wallId\": {\"type\": \"string\"},\n" + ITEM_PLACE
							+ ",\n\"depthMm\": {\"type\": \"number\"}", "itemId"),
					Set.of("updateItem"),
					(design, args, context) -> new DesignTool.Outcome(
							DesignEdits.updateItem(design, item(design, args), itemChanges(args, true)), null)),
			tool("remove_item", "Removes a window, doorway, appliance or filler.",
					schema("\"itemId\": {\"type\": \"string\"}", "itemId"), Set.of("removeItem"),
					(design, args, context) -> new DesignTool.Outcome(DesignEdits.removeItem(design, item(design, args)), null)),
			tool("add_cabinet", """
					Adds a cabinet from a template, at the template's newest version and default size. Without offsetMm it goes \
					at the first spot along the wall where it fits; sizes given replace the defaults.""",
					schema("\"templateCode\": {\"type\": \"string\"},\n\"wallId\": {\"type\": \"string\"},\n" + CABINET_SIZE,
							"templateCode", "wallId"),
					Set.of("addCabinet"),
					(design, args, context) -> {
						var code = text(args, "templateCode");
						var template = Optional.ofNullable(context.addableTemplates().get(code))
								.orElseThrow(() -> new IllegalArgumentException("no template " + code + " can be added"));
						var id = context.newId().apply("cabinet");
						var added = DesignEdits.addCabinet(design, template, wall(design, args), context.templates(), id);
						return new DesignTool.Outcome(DesignEdits.updateCabinet(added, id, cabinet -> sized(cabinet, args)), id);
					}),
			tool("update_cabinet", """
					Changes a cabinet: its wall, place, size, shelves, mirror, fronts (made in the shop or ordered, and \
					which ordered front style), finished end panels and crown moulding. Its template stays the same.""",
					schema("\"cabinetId\": {\"type\": \"string\"},\n\"wallId\": {\"type\": \"string\"},\n" + CABINET_SIZE + """
							,
							"frontSource": {"type": "string", "enum": ["MADE", "ORDERED"]},
							"orderedFrontStyle": {"type": ["string", "null"], "description": "Name of an ordered front style; null for the Construction Standard's"},
							"finishedLeft": {"type": "boolean"},
							"finishedRight": {"type": "boolean"},
							"crown": {"type": "boolean", "description": "Crown moulding, for wall and tall cabinets"}""", "cabinetId"),
					Set.of("updateCabinet"),
					(design, args, context) -> new DesignTool.Outcome(DesignEdits.updateCabinet(design, cabinet(design, args), cabinet -> {
						var changed = sized(cabinet, args).toBuilder()
								.wallId(optionalText(args, "wallId").map(wallId -> wall(design, wallId)).orElse(cabinet.getWallId()))
								.frontSource(optionalText(args, "frontSource").map(value -> choice(value, FrontSource.class))
										.orElse(cabinet.getFrontSource()))
								.finishedLeft(flag(args, "finishedLeft").orElse(cabinet.isFinishedLeft()))
								.finishedRight(flag(args, "finishedRight").orElse(cabinet.isFinishedRight()))
								.crown(flag(args, "crown").orElse(cabinet.isCrown()));
						if (args.has("orderedFrontStyle")) {
							changed.orderedFrontId(optionalText(args, "orderedFrontStyle")
									.map(name -> Optional.ofNullable(context.frontStyles().get(name))
											.orElseThrow(() -> new IllegalArgumentException("no ordered front style named " + name)))
									.orElse(null));
						}
						return changed.build();
					}), null)),
			tool("add_divider", "Adds a vertical divider in the middle of the cabinet's widest section.",
					schema("\"cabinetId\": {\"type\": \"string\"}", "cabinetId"), Set.of("addDivider"),
					(design, args, context) -> new DesignTool.Outcome(DesignEdits.addDivider(design, cabinet(design, args)), null)),
			tool("move_divider", "Moves a divider, by its index from the left, to a distance from the cabinet's left side.",
					schema("\"cabinetId\": {\"type\": \"string\"},\n\"index\": {\"type\": \"integer\"},\n\"positionMm\": {\"type\": \"number\"}",
							"cabinetId", "index", "positionMm"),
					Set.of("moveDivider"),
					(design, args, context) -> new DesignTool.Outcome(DesignEdits.moveDivider(design, cabinet(design, args),
							dividerIndex(design, args), number(args, "positionMm").orElseThrow()), null)),
			tool("remove_divider", "Removes a divider, by its index from the left.",
					schema("\"cabinetId\": {\"type\": \"string\"},\n\"index\": {\"type\": \"integer\"}", "cabinetId", "index"),
					Set.of("removeDivider"),
					(design, args, context) -> new DesignTool.Outcome(
							DesignEdits.removeDivider(design, cabinet(design, args), dividerIndex(design, args)), null)),
			tool("remove_cabinet", "Removes a cabinet.",
					schema("\"cabinetId\": {\"type\": \"string\"}", "cabinetId"), Set.of("removeCabinet"),
					(design, args, context) -> new DesignTool.Outcome(DesignEdits.removeCabinet(design, cabinet(design, args)), null)));

	public static final Map<String, DesignTool> BY_NAME = ALL.stream()
			.collect(Collectors.toUnmodifiableMap(DesignTool::name, Function.identity()));

	private DesignTools() {
	}

	private interface Applier {
		DesignTool.Outcome apply(Design design, JsonNode arguments, DesignToolContext context);
	}

	private static DesignTool tool(String name, String description, String inputSchema, Set<String> editorActions, Applier applier) {
		return new DesignTool() {
			@Override
			public String name() {
				return name;
			}

			@Override
			public String description() {
				return description;
			}

			@Override
			public String inputSchema() {
				return inputSchema;
			}

			@Override
			public Set<String> editorActions() {
				return editorActions;
			}

			@Override
			public Outcome apply(Design design, JsonNode arguments, DesignToolContext context) {
				return applier.apply(design, arguments, context);
			}
		};
	}

	private static String schema(String properties, String... required) {
		return "{\"type\": \"object\", \"properties\": {\n" + properties + "\n}, \"required\": ["
				+ java.util.Arrays.stream(required).map(name -> "\"" + name + "\"").collect(Collectors.joining(", "))
				+ "], \"additionalProperties\": false}";
	}

	private static Cabinet sized(Cabinet cabinet, JsonNode args) {
		return cabinet.toBuilder()
				.offsetMm(number(args, "offsetMm").orElse(cabinet.getOffsetMm()))
				.widthMm(number(args, "widthMm").orElse(cabinet.getWidthMm()))
				.heightMm(number(args, "heightMm").orElse(cabinet.getHeightMm()))
				.depthMm(number(args, "depthMm").orElse(cabinet.getDepthMm()))
				.elevationMm(number(args, "elevationMm").orElse(cabinet.getElevationMm()))
				.shelfCount(number(args, "shelfCount").map(Double::intValue).orElse(cabinet.getShelfCount()))
				.mirrored(flag(args, "mirrored").orElse(cabinet.isMirrored()))
				.build();
	}

	private static ItemChanges itemChanges(JsonNode args, boolean moveWall) {
		return new ItemChanges(moveWall ? optionalText(args, "wallId").orElse(null) : null,
				number(args, "offsetMm").orElse(null), number(args, "widthMm").orElse(null), number(args, "heightMm").orElse(null),
				number(args, "depthMm").orElse(null), number(args, "elevationMm").orElse(null));
	}

	private static String wall(Design design, JsonNode args) {
		return wall(design, text(args, "wallId"));
	}

	private static String wall(Design design, String wallId) {
		if (Optional.ofNullable(design.getWalls()).orElse(List.of()).stream().noneMatch(wall -> wall.getId().equals(wallId))) {
			throw new IllegalArgumentException("no wall " + wallId);
		}
		return wallId;
	}

	private static String cabinet(Design design, JsonNode args) {
		var cabinetId = text(args, "cabinetId");
		if (Optional.ofNullable(design.getCabinets()).orElse(List.of()).stream().noneMatch(cabinet -> cabinet.getId().equals(cabinetId))) {
			throw new IllegalArgumentException("no cabinet " + cabinetId);
		}
		return cabinetId;
	}

	private static int dividerIndex(Design design, JsonNode args) {
		var cabinetId = cabinet(design, args);
		var index = number(args, "index").map(Double::intValue).orElseThrow(() -> new IllegalArgumentException("index is required"));
		var count = design.getCabinets().stream().filter(cabinet -> cabinet.getId().equals(cabinetId)).findFirst().orElseThrow()
				.dividers().size();
		if (index < 0 || index >= count) {
			throw new IllegalArgumentException("cabinet " + cabinetId + " has " + count + " divider(s)");
		}
		return index;
	}

	private static String item(Design design, JsonNode args) {
		var itemId = text(args, "itemId");
		var exists = Optional.ofNullable(design.getOpenings()).orElse(List.of()).stream().anyMatch(item -> item.getId().equals(itemId))
				|| Optional.ofNullable(design.getAppliances()).orElse(List.of()).stream().anyMatch(item -> item.getId().equals(itemId))
				|| Optional.ofNullable(design.getFillers()).orElse(List.of()).stream().anyMatch(item -> item.getId().equals(itemId));
		if (!exists) {
			throw new IllegalArgumentException("no window, doorway, appliance or filler " + itemId);
		}
		return itemId;
	}

	private static String text(JsonNode args, String field) {
		return optionalText(args, field).orElseThrow(() -> new IllegalArgumentException(field + " is required"));
	}

	private static Optional<String> optionalText(JsonNode args, String field) {
		return Optional.ofNullable(args.get(field)).filter(JsonNode::isString).map(JsonNode::asString);
	}

	private static Optional<Double> number(JsonNode args, String field) {
		var value = args.get(field);
		if (value == null || value.isNull()) {
			return Optional.empty();
		}
		if (!value.isNumber()) {
			throw new IllegalArgumentException(field + " must be a number");
		}
		return Optional.of(value.asDouble());
	}

	private static Optional<Boolean> flag(JsonNode args, String field) {
		var value = args.get(field);
		if (value == null || value.isNull()) {
			return Optional.empty();
		}
		if (!value.isBoolean()) {
			throw new IllegalArgumentException(field + " must be true or false");
		}
		return Optional.of(value.asBoolean());
	}

	private static <E extends Enum<E>> E choice(JsonNode args, String field, Class<E> type) {
		return choice(text(args, field), type);
	}

	private static <E extends Enum<E>> E choice(String value, Class<E> type) {
		try {
			return Enum.valueOf(type, value);
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException(value + " is not one of " + java.util.Arrays.toString(type.getEnumConstants()));
		}
	}
}
