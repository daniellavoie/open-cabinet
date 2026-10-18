package dev.daniellavoie.opencabinet.drawing.domain;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.catalog.domain.port.api.CatalogDomain;
import dev.daniellavoie.opencabinet.client.domain.port.api.ClientDomain;
import dev.daniellavoie.opencabinet.cutlist.domain.port.api.CutListDomain;
import dev.daniellavoie.opencabinet.drawing.domain.model.DrawingLanguage;
import dev.daniellavoie.opencabinet.drawing.domain.model.ShopDrawings;
import dev.daniellavoie.opencabinet.drawing.domain.model.View;
import dev.daniellavoie.opencabinet.drawing.domain.port.api.ShopDrawingsDomain;
import dev.daniellavoie.opencabinet.drawing.domain.port.backend.ShopDrawingsRenderer;
import dev.daniellavoie.opencabinet.member.domain.port.api.MemberDomain;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.Project;
import dev.daniellavoie.opencabinet.project.domain.port.api.ProjectDomain;
import dev.daniellavoie.opencabinet.shop.domain.model.ConstructionStandard;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;
import dev.daniellavoie.opencabinet.shop.domain.port.backend.ShopRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultShopDrawingsDomain implements ShopDrawingsDomain {
	MemberDomain memberDomain;
	ProjectDomain projectDomain;
	ClientDomain clientDomain;
	CatalogDomain catalogDomain;
	CutListDomain cutListDomain;
	ShopRepository shopRepository;
	ShopDrawingsRenderer renderer;

	@Override
	public ShopDrawings drawings(UUID projectId, DrawingLanguage language, Principal principal) {
		var shopId = memberDomain.current(principal).getShopId();
		var shop = shopRepository.findById(shopId);
		var project = projectDomain.findById(projectId, shopId);
		var standard = shop.getSettings().getConstructionStandard();
		var catalog = catalogDomain.findByShop(shopId);
		var output = cutListDomain.output(project, standard, catalog);

		var templates = templates(project);
		Map<UUID, Material> materials = catalog.stream()
				.collect(Collectors.toMap(Material::getId, Function.identity()));

		return ShopDrawings.builder()
				.shopName(shop.getName())
				.projectName(project.getName())
				.clientName(clientDomain.findById(project.getClientId(), shopId).getName())
				.date(LocalDate.now())
				.units(shop.getUnits())
				.language(language)
				.sheets(ShopDrawingsGenerator.generate(new ShopDrawingsGenerator.Source(project.getDesign(), templates,
						output.getCabinetLabels(), output.getParts(), materials, standard, shop.getUnits(), language)))
				.build();
	}

	@Override
	public List<View> quotePictures(Project project, ConstructionStandard standard, Units units,
			List<Material> catalog) {
		return QuotePicturesGenerator.generate(new ShopDrawingsGenerator.Source(project.getDesign(), templates(project),
				Map.of(), List.of(), catalog.stream().collect(Collectors.toMap(Material::getId, Function.identity())),
				standard, units, DrawingLanguage.FR));
	}

	@Override
	public byte[] pdf(UUID projectId, DrawingLanguage language, Principal principal) {
		return renderer.pdf(drawings(projectId, language, principal));
	}

	private Map<String, CabinetTemplate> templates(Project project) {
		return projectDomain.templates(project).stream()
				.collect(Collectors.toMap(CabinetTemplate::key, Function.identity()));
	}
}
