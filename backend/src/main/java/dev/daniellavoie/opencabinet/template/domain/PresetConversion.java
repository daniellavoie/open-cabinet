package dev.daniellavoie.opencabinet.template.domain;

import java.util.Comparator;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.port.api.ProjectDomain;
import dev.daniellavoie.opencabinet.template.domain.model.ProjectTemplate;
import dev.daniellavoie.opencabinet.template.domain.model.ShopCabinetTemplate;
import dev.daniellavoie.opencabinet.template.domain.port.backend.ProjectTemplateRepository;
import dev.daniellavoie.opencabinet.template.domain.port.backend.ShopCabinetTemplateRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Turns Milestone 3's Shop presets into Shop library templates: each starts from the newest
 * assembled version of its starter template, takes the preset's size and shelves as its defaults,
 * is published as version 1 and shared, then the preset is removed. A preset's other choices (made
 * or ordered Fronts, finished ends, crown, dividers) belong to Cabinets, not templates, and are not
 * kept.
 */
@Slf4j
@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PresetConversion {
	ShopCabinetTemplateRepository presets;
	ProjectTemplateRepository templates;
	ProjectDomain projectDomain;

	/**
	 * Converts every preset left; does nothing once there are none.
	 */
	public int convertAll() {
		var converted = 0;
		for (var shopId : presets.findShopsWithPresets()) {
			for (var preset : presets.findByShop(shopId)) {
				convert(preset, shopId);
				presets.delete(preset.getId(), shopId);
				converted++;
			}
		}
		if (converted > 0) {
			log.info("Converted {} shop preset(s) into shop library templates", converted);
		}
		return converted;
	}

	private void convert(ShopCabinetTemplate preset, UUID shopId) {
		var source = projectDomain.templates().stream()
				.filter(template -> template.getCode().equals(preset.getBaseCode()) && template.getAssembly() != null)
				.max(Comparator.comparingInt(CabinetTemplate::getVersion))
				.orElseThrow(() -> new IllegalStateException("no assembled version of " + preset.getBaseCode()));
		var cabinet = preset.getPreset();
		var name = libraryName(preset.getName(), shopId);
		var draft = source.toBuilder()
				.code(null)
				.version(0)
				.name(name)
				.changeNote(null)
				.defaultWidthMm(cabinet.getWidthMm())
				.defaultHeightMm(cabinet.getHeightMm())
				.defaultDepthMm(cabinet.getDepthMm())
				.defaultElevationMm(cabinet.getElevationMm())
				.defaultShelfCount(cabinet.getShelfCount())
				.minWidthMm(Math.min(source.getMinWidthMm(), cabinet.getWidthMm()))
				.maxWidthMm(Math.max(source.getMaxWidthMm(), cabinet.getWidthMm()))
				.minHeightMm(Math.min(source.getMinHeightMm(), cabinet.getHeightMm()))
				.maxHeightMm(Math.max(source.getMaxHeightMm(), cabinet.getHeightMm()))
				.minDepthMm(Math.min(source.getMinDepthMm(), cabinet.getDepthMm()))
				.maxDepthMm(Math.max(source.getMaxDepthMm(), cabinet.getDepthMm()))
				.build();

		var id = UUID.randomUUID();
		templates.save(ProjectTemplate.builder().id(id).name(name).draft(draft).shared(true).build(), shopId);
		templates.addVersion(id, draft.toBuilder().code(id.toString()).version(1).build(), shopId);
	}

	/**
	 * The preset's name, or with a number when the library already has a template by that name.
	 */
	private String libraryName(String name, UUID shopId) {
		var taken = templates.findShared(shopId).stream().map(template -> template.getName().toLowerCase()).toList();
		var candidate = name;
		for (int index = 2; taken.contains(candidate.toLowerCase()); index++) {
			candidate = name + " (" + index + ")";
		}
		return candidate;
	}
}
