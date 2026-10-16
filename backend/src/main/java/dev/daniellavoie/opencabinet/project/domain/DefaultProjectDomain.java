package dev.daniellavoie.opencabinet.project.domain;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import dev.daniellavoie.opencabinet.client.domain.port.api.ClientDomain;
import dev.daniellavoie.opencabinet.member.domain.port.api.MemberDomain;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.ConstructionRuleException;
import dev.daniellavoie.opencabinet.project.domain.model.CreateProjectRequest;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.ManualLine;
import dev.daniellavoie.opencabinet.project.domain.model.Project;
import dev.daniellavoie.opencabinet.project.domain.model.UpdateProjectRequest;
import dev.daniellavoie.opencabinet.project.domain.model.Wall;
import dev.daniellavoie.opencabinet.project.domain.port.api.ProjectDomain;
import dev.daniellavoie.opencabinet.project.domain.port.backend.CabinetTemplateRepository;
import dev.daniellavoie.opencabinet.project.domain.port.backend.ProjectRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultProjectDomain implements ProjectDomain {
	private static final double FOOT = 304.8;

	ProjectRepository repository;
	CabinetTemplateRepository templateRepository;
	MemberDomain memberDomain;
	ClientDomain clientDomain;

	@Override
	public Page<Project> findByPrincipal(Principal principal, Pageable pageable) {
		return repository.findByShop(memberDomain.current(principal).getShopId(), pageable);
	}

	@Override
	public Project findById(UUID projectId, Principal principal) {
		return findById(projectId, memberDomain.current(principal).getShopId());
	}

	@Override
	public Project findById(UUID projectId, UUID shopId) {
		return repository.findById(projectId, shopId);
	}

	@Override
	public Project create(CreateProjectRequest request, Principal principal) {
		var shopId = memberDomain.current(principal).getShopId();
		var clientId = Optional.ofNullable(request.getClientId())
				.orElseThrow(() -> new IllegalArgumentException("a client is required"));
		clientDomain.findById(clientId, shopId);

		return repository.save(Project.builder()
				.id(UUID.randomUUID())
				.clientId(clientId)
				.name(requireName(request.getName()))
				.design(Design.builder()
						.walls(List.of(Wall.builder()
								.id("wall-1")
								.lengthMm(12 * FOOT)
								.heightMm(8 * FOOT)
								.angleToNextDegrees(90)
								.build()))
						.cabinets(List.of())
						.build())
				.manualLines(List.of())
				.build(), shopId);
	}

	@Override
	public Project update(UUID projectId, UpdateProjectRequest request, Principal principal) {
		var shopId = memberDomain.current(principal).getShopId();
		var project = repository.findById(projectId, shopId);

		var clientId = Optional.ofNullable(request.getClientId())
				.orElseThrow(() -> new IllegalArgumentException("a client is required"));
		clientDomain.findById(clientId, shopId);

		var design = Optional.ofNullable(request.getDesign())
				.orElseThrow(() -> new IllegalArgumentException("a design is required"));
		requireIds(design);

		var violations = ConstructionRules.check(design, templateRepository.findAll());
		if (!violations.isEmpty()) {
			throw new ConstructionRuleException(violations);
		}

		var labourHours = request.getLabourHours();
		if (labourHours != null && labourHours.signum() < 0) {
			throw new IllegalArgumentException("labour hours cannot be negative");
		}

		var manualLines = Optional.ofNullable(request.getManualLines()).orElse(List.of());
		manualLines.forEach(DefaultProjectDomain::validate);

		return repository.save(project.toBuilder()
				.clientId(clientId)
				.name(requireName(request.getName()))
				.design(design.toBuilder()
						.walls(List.copyOf(design.getWalls()))
						.cabinets(Optional.ofNullable(design.getCabinets()).map(List::copyOf).orElse(List.of()))
						.build())
				.labourMethod(request.getLabourMethod())
				.labourHours(labourHours)
				.manualLines(List.copyOf(manualLines))
				.build(), shopId);
	}

	@Override
	public List<CabinetTemplate> templates() {
		return templateRepository.findAll();
	}

	private static String requireName(String name) {
		return Optional.ofNullable(name)
				.filter(value -> !value.isBlank())
				.orElseThrow(() -> new IllegalArgumentException("a project name is required"));
	}

	private static void requireIds(Design design) {
		var walls = Optional.ofNullable(design.getWalls())
				.filter(value -> !value.isEmpty())
				.orElseThrow(() -> new IllegalArgumentException("a room needs at least one wall"));
		var ids = new HashSet<String>();

		walls.forEach(wall -> requireUniqueId(wall.getId(), ids));
		Optional.ofNullable(design.getCabinets())
				.orElse(List.of())
				.forEach(cabinet -> requireUniqueId(cabinet.getId(), ids));
	}

	private static void requireUniqueId(String id, HashSet<String> ids) {
		if (id == null || id.isBlank() || !ids.add(id)) {
			throw new IllegalArgumentException("every wall and cabinet needs a unique id");
		}
	}

	private static void validate(ManualLine line) {
		if (line.getDescription() == null || line.getDescription().isBlank()) {
			throw new IllegalArgumentException("every manual line needs a description");
		}
		Optional.ofNullable(line.getAmount())
				.map(BigDecimal::signum)
				.orElseThrow(() -> new IllegalArgumentException("every manual line needs an amount"));
	}
}
