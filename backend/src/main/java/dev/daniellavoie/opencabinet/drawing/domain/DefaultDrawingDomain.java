package dev.daniellavoie.opencabinet.drawing.domain;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import dev.daniellavoie.opencabinet.catalog.domain.port.api.CatalogDomain;
import dev.daniellavoie.opencabinet.client.domain.port.api.ClientDomain;
import dev.daniellavoie.opencabinet.common.domain.Language;
import dev.daniellavoie.opencabinet.cutlist.domain.port.api.CutListDomain;
import dev.daniellavoie.opencabinet.drawing.domain.model.ShopDrawingSet;
import dev.daniellavoie.opencabinet.drawing.domain.model.ShopDrawingSheet;
import dev.daniellavoie.opencabinet.drawing.domain.port.api.DrawingDomain;
import dev.daniellavoie.opencabinet.drawing.domain.port.backend.ShopDrawingsRenderer;
import dev.daniellavoie.opencabinet.member.domain.port.api.MemberDomain;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.port.api.ProjectDomain;
import dev.daniellavoie.opencabinet.shop.domain.port.backend.ShopRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultDrawingDomain implements DrawingDomain {
	MemberDomain memberDomain;
	ProjectDomain projectDomain;
	ClientDomain clientDomain;
	CatalogDomain catalogDomain;
	CutListDomain cutListDomain;
	ShopRepository shopRepository;
	ShopDrawingsRenderer renderer;

	@Override
	public ShopDrawingSet shopDrawings(UUID projectId, Language language, Principal principal) {
		var shopId = memberDomain.current(principal).getShopId();
		var shop = shopRepository.findById(shopId);
		var project = projectDomain.findById(projectId, shopId);
		var standard = shop.getSettings().getConstructionStandard();
		var templates = projectDomain.templates().stream()
				.collect(Collectors.toMap(CabinetTemplate::getCode, Function.identity()));
		var context = DrawingContext.of(project.getDesign(), templates, standard.getToeKickHeightMm(),
				standard.getDoorGapMm(), shop.getUnits(), language);
		var cutLists = cutListDomain.cabinetCutLists(project, standard, catalogDomain.findByShop(shopId));

		var sheets = new ArrayList<ShopDrawingSheet>();
		sheets.add(new ShopDrawingSheet(ShopDrawingSheet.Kind.PLAN, ShopDrawings.plan(context), List.of()));
		IntStream.range(0, context.walls().size())
				.forEach(index -> sheets.add(new ShopDrawingSheet(ShopDrawingSheet.Kind.ELEVATION,
						ShopDrawings.elevation(context, index), List.of())));
		context.cabinets().stream()
				.filter(cabinet -> templates.containsKey(cabinet.getTemplateCode()))
				.forEach(cabinet -> sheets.add(new ShopDrawingSheet(ShopDrawingSheet.Kind.CABINET,
						ShopDrawings.cabinet(context, cabinet),
						cutLists.getOrDefault(context.label(cabinet), List.of()))));

		return new ShopDrawingSet(shop.getName(), project.getName(),
				clientDomain.findById(project.getClientId(), shopId).getName(), shop.getUnits(), language, sheets);
	}

	@Override
	public byte[] shopDrawingsPdf(UUID projectId, Language language, Principal principal) {
		return renderer.pdf(shopDrawings(projectId, language, principal));
	}
}
