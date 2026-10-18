package dev.daniellavoie.opencabinet.template.domain;

import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.catalog.domain.model.MaterialKind;
import dev.daniellavoie.opencabinet.catalog.domain.port.api.CatalogDomain;
import dev.daniellavoie.opencabinet.member.domain.port.api.MemberDomain;
import dev.daniellavoie.opencabinet.project.domain.CabinetAssemblies;
import dev.daniellavoie.opencabinet.project.domain.TemplateRules;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.MaterialRef;
import dev.daniellavoie.opencabinet.project.domain.port.api.ProjectDomain;
import dev.daniellavoie.opencabinet.shop.domain.port.backend.ShopRepository;
import dev.daniellavoie.opencabinet.template.domain.model.CreateProjectTemplateRequest;
import dev.daniellavoie.opencabinet.template.domain.model.ProjectTemplate;
import dev.daniellavoie.opencabinet.template.domain.model.PublishTemplateRequest;
import dev.daniellavoie.opencabinet.template.domain.model.TemplateRuleException;
import dev.daniellavoie.opencabinet.template.domain.model.UpdateProjectTemplateRequest;
import dev.daniellavoie.opencabinet.template.domain.port.api.ProjectTemplateDomain;
import dev.daniellavoie.opencabinet.template.domain.port.backend.ProjectTemplateRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultProjectTemplateDomain implements ProjectTemplateDomain {
	MemberDomain memberDomain;
	ProjectDomain projectDomain;
	CatalogDomain catalogDomain;
	ShopRepository shopRepository;
	ProjectTemplateRepository repository;

	@Override
	public List<ProjectTemplate> findByProject(UUID projectId, Principal principal) {
		var shopId = memberDomain.current(principal).getShopId();
		projectDomain.findById(projectId, shopId);
		return repository.findByProject(projectId, shopId);
	}

	@Override
	public ProjectTemplate findById(UUID templateId, Principal principal) {
		return repository.findById(templateId, memberDomain.current(principal).getShopId());
	}

	@Override
	public ProjectTemplate create(UUID projectId, CreateProjectTemplateRequest request, Principal principal) {
		var shopId = memberDomain.current(principal).getShopId();
		var project = projectDomain.findById(projectId, shopId);
		var name = requireUniqueName(request.getName(), null, projectId, false, shopId);
		var templates = projectDomain.templates(project);

		var cabinet = Optional.ofNullable(request.getCabinetId())
				.map(cabinetId -> Optional.ofNullable(project.getDesign().getCabinets()).orElse(List.of()).stream()
						.filter(candidate -> candidate.getId().equals(cabinetId))
						.findFirst()
						.orElseThrow(() -> new IllegalArgumentException("cabinet " + cabinetId + " not found")));
		var code = cabinet.map(found -> found.getTemplateCode()).orElse(request.getTemplateCode());
		var version = cabinet.map(found -> found.getTemplateVersion()).orElse(request.getTemplateVersion());
		var source = assembled(templates, Optional.ofNullable(code)
				.orElseThrow(() -> new IllegalArgumentException("a template or a cabinet to start from is required")),
				version);

		var draft = source.toBuilder().code(null).version(0).name(name).changeNote(null);
		cabinet.ifPresent(found -> draft
				.defaultWidthMm(found.getWidthMm())
				.defaultHeightMm(found.getHeightMm())
				.defaultDepthMm(found.getDepthMm())
				.defaultElevationMm(found.getElevationMm())
				.defaultShelfCount(found.getShelfCount())
				.minWidthMm(Math.min(source.getMinWidthMm(), found.getWidthMm()))
				.maxWidthMm(Math.max(source.getMaxWidthMm(), found.getWidthMm()))
				.minHeightMm(Math.min(source.getMinHeightMm(), found.getHeightMm()))
				.maxHeightMm(Math.max(source.getMaxHeightMm(), found.getHeightMm()))
				.minDepthMm(Math.min(source.getMinDepthMm(), found.getDepthMm()))
				.maxDepthMm(Math.max(source.getMaxDepthMm(), found.getDepthMm())));

		return repository.save(ProjectTemplate.builder()
				.id(UUID.randomUUID())
				.projectId(projectId)
				.name(name)
				.draft(draft.build())
				.sourceCabinetId(cabinet.map(found -> found.getId()).orElse(null))
				.build(), shopId);
	}

	@Override
	public ProjectTemplate update(UUID templateId, UpdateProjectTemplateRequest request, Principal principal) {
		var shopId = memberDomain.current(principal).getShopId();
		var template = repository.findById(templateId, shopId);
		var name = requireUniqueName(request.getName(), templateId, template.getProjectId(), template.isShared(), shopId);
		var draft = Optional.ofNullable(request.getDraft())
				.orElseThrow(() -> new IllegalArgumentException("a draft is required"));

		return repository.save(template.toBuilder()
				.name(name)
				.draft(draft.toBuilder().code(null).version(0).name(name).changeNote(null).build())
				.build(), shopId);
	}

	@Override
	public ProjectTemplate publish(UUID templateId, PublishTemplateRequest request, Principal principal) {
		var shopId = memberDomain.current(principal).getShopId();
		var template = repository.findById(templateId, shopId);
		var draft = template.getDraft();
		var next = template.getVersions().size() + 1;
		var note = Optional.ofNullable(request.getChangeNote()).map(String::trim).filter(text -> !text.isEmpty());
		if (next > 1 && note.isEmpty()) {
			throw new IllegalArgumentException("say what changed from version " + (next - 1));
		}
		requireBuildable(draft);

		var violations = TemplateRules.check(draft, shop(shopId));
		if (!violations.isEmpty()) {
			throw new TemplateRuleException(violations);
		}

		repository.addVersion(templateId, draft.toBuilder()
				.code(templateId.toString())
				.version(next)
				.name(template.getName())
				.changeNote(note.orElse(null))
				.build(), shopId);
		return repository.findById(templateId, shopId);
	}

	@Override
	public List<ProjectTemplate> findShared(Principal principal) {
		return repository.findShared(memberDomain.current(principal).getShopId());
	}

	@Override
	public ProjectTemplate createInLibrary(CreateProjectTemplateRequest request, Principal principal) {
		var shopId = memberDomain.current(principal).getShopId();
		var name = requireUniqueName(request.getName(), null, null, true, shopId);
		var templates = Stream.concat(projectDomain.templates().stream(),
				repository.findShared(shopId).stream().flatMap(template -> template.getVersions().stream())).toList();
		var source = assembled(templates, Optional.ofNullable(request.getTemplateCode())
				.orElseThrow(() -> new IllegalArgumentException("a template to start from is required")),
				request.getTemplateVersion());

		return repository.save(ProjectTemplate.builder()
				.id(UUID.randomUUID())
				.name(name)
				.draft(source.toBuilder().code(null).version(0).name(name).changeNote(null).archived(null).build())
				.shared(true)
				.build(), shopId);
	}

	@Override
	public ProjectTemplate share(UUID templateId, Principal principal) {
		var shopId = memberDomain.current(principal).getShopId();
		var template = repository.findById(templateId, shopId);
		if (template.isShared()) {
			throw new IllegalStateException("this template is already in the shop library");
		}
		requireUniqueName(template.getName(), templateId, null, true, shopId);
		repository.share(templateId, shopId);
		return repository.findById(templateId, shopId);
	}

	@Override
	public ProjectTemplate setArchived(UUID templateId, boolean archived, Principal principal) {
		var shopId = memberDomain.current(principal).getShopId();
		repository.findById(templateId, shopId);
		repository.setArchived(templateId, archived, shopId);
		return repository.findById(templateId, shopId);
	}

	@Override
	public void delete(UUID templateId, Principal principal) {
		var shopId = memberDomain.current(principal).getShopId();
		if (!repository.findById(templateId, shopId).getVersions().isEmpty()) {
			throw new IllegalStateException("a published template is archived, not deleted");
		}
		repository.delete(templateId, shopId);
	}

	/**
	 * The template to start from, or the newest version of the same template drawn as an assembly
	 * when that one is still built by the fixed recipe.
	 */
	private static CabinetTemplate assembled(List<CabinetTemplate> templates, String code, Integer version) {
		var versions = templates.stream().filter(template -> template.getCode().equals(code)).toList();
		return versions.stream()
				.filter(template -> version != null && template.getVersion() == version && template.getAssembly() != null)
				.findFirst()
				.or(() -> versions.stream()
						.filter(template -> template.getAssembly() != null)
						.max(Comparator.comparingInt(CabinetTemplate::getVersion)))
				.orElseThrow(() -> new IllegalArgumentException("template " + code + " is not drawn as parts"));
	}

	/**
	 * A name unique where the template lives: its Project, or the Shop library once shared.
	 */
	private String requireUniqueName(String requested, UUID templateId, UUID projectId, boolean shared, UUID shopId) {
		var name = Optional.ofNullable(requested).map(String::trim).filter(text -> !text.isEmpty())
				.orElseThrow(() -> new IllegalArgumentException("a template name is required"));
		var neighbours = shared ? repository.findShared(shopId) : repository.findByProject(projectId, shopId);
		if (neighbours.stream()
				.anyMatch(other -> !other.getId().equals(templateId) && other.getName().equalsIgnoreCase(name))) {
			throw new IllegalArgumentException((shared ? "the shop library" : "this project")
					+ " already has a template named " + name);
		}
		return name;
	}

	/**
	 * A template must be drawn as parts, of a type, with sizes and shelf counts that make sense.
	 */
	private static void requireBuildable(CabinetTemplate draft) {
		if (draft == null || draft.getAssembly() == null || draft.getType() == null) {
			throw new IllegalArgumentException("a template needs a type and parts");
		}
		requireRange("width", draft.getMinWidthMm(), draft.getDefaultWidthMm(), draft.getMaxWidthMm());
		requireRange("height", draft.getMinHeightMm(), draft.getDefaultHeightMm(), draft.getMaxHeightMm());
		requireRange("depth", draft.getMinDepthMm(), draft.getDefaultDepthMm(), draft.getMaxDepthMm());
		if (draft.getDefaultShelfCount() < 0 || draft.getDefaultShelfCount() > draft.getMaxShelfCount()) {
			throw new IllegalArgumentException("the default shelf count must be between 0 and the most shelves");
		}
	}

	private static void requireRange(String dimension, double minimum, double fallback, double maximum) {
		if (minimum <= 0 || fallback < minimum || fallback > maximum) {
			throw new IllegalArgumentException("the " + dimension
					+ " range must be positive, with the default between the minimum and maximum");
		}
	}

	/**
	 * The Shop's materials as the template rules see them: thicknesses, and sheets for sheet goods.
	 */
	private TemplateRules.Shop shop(UUID shopId) {
		var standard = shopRepository.findById(shopId).getSettings().getConstructionStandard();
		Map<UUID, Material> materials = catalogDomain.findByShop(shopId).stream()
				.collect(Collectors.toMap(Material::getId, Function.identity()));
		Function<MaterialRef, Optional<Material>> material =
				reference -> Optional.ofNullable(materials.get(CabinetAssemblies.materialId(reference, standard)));
		return new TemplateRules.Shop(CabinetAssemblies.standardValues(standard),
				reference -> material.apply(reference).map(Material::getThicknessMm).orElse(0d),
				reference -> material.apply(reference)
						.filter(found -> found.getKind() == MaterialKind.SHEET && found.getLengthMm() != null
								&& found.getWidthMm() != null)
						.map(found -> new TemplateRules.Sheet(found.getLengthMm(), found.getWidthMm(), found.isGrain()))
						.orElse(null),
				standard.getSheetTrimMm());
	}
}
