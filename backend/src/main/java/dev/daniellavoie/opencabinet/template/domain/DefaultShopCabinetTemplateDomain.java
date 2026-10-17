package dev.daniellavoie.opencabinet.template.domain;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dev.daniellavoie.opencabinet.member.domain.port.api.MemberDomain;
import dev.daniellavoie.opencabinet.project.domain.ConstructionRules;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.Violation;
import dev.daniellavoie.opencabinet.project.domain.model.Wall;
import dev.daniellavoie.opencabinet.project.domain.port.api.ProjectDomain;
import dev.daniellavoie.opencabinet.template.domain.model.ShopCabinetTemplate;
import dev.daniellavoie.opencabinet.template.domain.model.ShopCabinetTemplateRequest;
import dev.daniellavoie.opencabinet.template.domain.port.api.ShopCabinetTemplateDomain;
import dev.daniellavoie.opencabinet.template.domain.port.backend.ShopCabinetTemplateRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultShopCabinetTemplateDomain implements ShopCabinetTemplateDomain {
	/**
	 * A room big enough that a preset can only break the rules of its own Cabinet.
	 */
	static final double CHECK_WALL_MM = 100_000;

	ShopCabinetTemplateRepository repository;
	MemberDomain memberDomain;
	ProjectDomain projectDomain;

	@Override
	public List<ShopCabinetTemplate> findAll(Principal principal) {
		return repository.findByShop(memberDomain.current(principal).getShopId());
	}

	@Override
	public ShopCabinetTemplate create(ShopCabinetTemplateRequest request, Principal principal) {
		var shopId = memberDomain.currentOwner(principal).getShopId();
		var name = Optional.ofNullable(request.getName())
				.map(String::trim)
				.filter(value -> !value.isEmpty())
				.orElseThrow(() -> new IllegalArgumentException("a template name is required"));
		var cabinet = Optional.ofNullable(request.getCabinet())
				.orElseThrow(() -> new IllegalArgumentException("a cabinet is required"));
		var preset = cabinet.toBuilder().id("preset").wallId("check").offsetMm(0).build();

		var violations = ConstructionRules.check(Design.builder()
				.walls(List.of(Wall.builder().id("check").lengthMm(CHECK_WALL_MM).heightMm(CHECK_WALL_MM).build()))
				.cabinets(List.of(preset))
				.build(), projectDomain.templates());
		if (!violations.isEmpty()) {
			throw new IllegalArgumentException("the cabinet breaks the construction rules: "
					+ String.join(", ", violations.stream().map(Violation::getCode).toList()));
		}

		return repository.save(ShopCabinetTemplate.builder()
				.id(UUID.randomUUID())
				.name(name)
				.baseCode(cabinet.getTemplateCode())
				.preset(preset.toBuilder().id(null).wallId(null).build())
				.build(), shopId);
	}

	@Override
	public void delete(UUID templateId, Principal principal) {
		repository.delete(templateId, memberDomain.currentOwner(principal).getShopId());
	}
}
