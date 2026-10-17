package dev.daniellavoie.opencabinet.shop.domain;

import java.math.BigDecimal;
import java.security.Principal;
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
import dev.daniellavoie.opencabinet.shop.domain.model.ConstructionStandard;
import dev.daniellavoie.opencabinet.shop.domain.model.CreateShopRequest;
import dev.daniellavoie.opencabinet.shop.domain.model.LabourMethod;
import dev.daniellavoie.opencabinet.shop.domain.model.LabourSettings;
import dev.daniellavoie.opencabinet.shop.domain.model.Shop;
import dev.daniellavoie.opencabinet.shop.domain.model.ShopSettings;
import dev.daniellavoie.opencabinet.shop.domain.model.Tax;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;
import dev.daniellavoie.opencabinet.shop.domain.model.UpdateShopRequest;
import dev.daniellavoie.opencabinet.shop.domain.port.api.ShopDomain;
import dev.daniellavoie.opencabinet.shop.domain.port.backend.ShopRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultShopDomain implements ShopDomain {
	ShopRepository repository;
	MemberDomain memberDomain;
	CatalogDomain catalogDomain;

	@Override
	public Shop create(CreateShopRequest request, Principal principal) {
		if (memberDomain.findCurrent(principal).isPresent()) {
			throw new IllegalStateException("you are already a member of a shop");
		}

		var name = Optional.ofNullable(request.getShopName())
				.filter(value -> !value.isBlank())
				.orElseThrow(() -> new IllegalArgumentException("a shop name is required"));

		var shop = repository.create(Shop.builder()
				.id(UUID.randomUUID())
				.name(name)
				.currency("CAD")
				.units(Units.IMPERIAL)
				.settings(defaultSettings(null))
				.build());

		memberDomain.createOwner(shop.getId(), request.getMemberName(), request.getMemberEmail(), principal);

		return repository.update(shop.toBuilder()
				.settings(defaultSettings(catalogDomain.seedDefaults(shop.getId())))
				.build());
	}

	@Override
	public Shop current(Principal principal) {
		return repository.findById(memberDomain.current(principal).getShopId());
	}

	@Override
	public Shop update(UpdateShopRequest request, Principal principal) {
		var owner = memberDomain.currentOwner(principal);
		var shop = repository.findById(owner.getShopId());

		var name = Optional.ofNullable(request.getName())
				.filter(value -> !value.isBlank())
				.orElseThrow(() -> new IllegalArgumentException("a shop name is required"));
		var currency = Optional.ofNullable(request.getCurrency())
				.filter(value -> value.matches("[A-Z]{3}"))
				.orElseThrow(() -> new IllegalArgumentException("a three-letter currency code is required"));
		var units = Optional.ofNullable(request.getUnits())
				.orElseThrow(() -> new IllegalArgumentException("units are required"));
		var settings = Optional.ofNullable(request.getSettings())
				.orElseThrow(() -> new IllegalArgumentException("settings are required"));

		validate(settings, catalogDomain.findByShop(shop.getId()));

		return repository.update(shop.toBuilder()
				.name(name)
				.currency(currency)
				.units(units)
				.settings(settings)
				.build());
	}

	@Override
	public int nextQuoteNumber(Principal principal) {
		return repository.nextQuoteNumber(memberDomain.current(principal).getShopId());
	}

	private static ShopSettings defaultSettings(ConstructionStandard constructionStandard) {
		return ShopSettings.builder()
				.markupPercent(new BigDecimal("25"))
				.taxes(List.of(
						Tax.builder().name("TPS").ratePercent(new BigDecimal("5")).build(),
						Tax.builder().name("TVQ").ratePercent(new BigDecimal("9.975")).build()))
				.labour(LabourSettings.builder()
						.defaultMethod(LabourMethod.OPERATION)
						.ratePerSheet(new BigDecimal("35.00"))
						.ratePerEdgeBandingMeter(new BigDecimal("1.50"))
						.ratePerCabinet(new BigDecimal("60.00"))
						.ratePerHardwareItem(new BigDecimal("4.00"))
						.ratePerLinearFoot(new BigDecimal("250.00"))
						.hourlyRate(new BigDecimal("65.00"))
						.build())
				.constructionStandard(constructionStandard)
				.build();
	}

	private static void validate(ShopSettings settings, List<Material> catalog) {
		nonNegative(settings.getMarkupPercent(), "markup");

		Optional.ofNullable(settings.getTaxes()).orElse(List.of()).forEach(tax -> {
			if (tax.getName() == null || tax.getName().isBlank()) {
				throw new IllegalArgumentException("every tax needs a name");
			}
			nonNegative(tax.getRatePercent(), "tax rate");
		});

		var labour = Optional.ofNullable(settings.getLabour())
				.orElseThrow(() -> new IllegalArgumentException("labour settings are required"));
		Optional.ofNullable(labour.getDefaultMethod())
				.orElseThrow(() -> new IllegalArgumentException("a default labour method is required"));
		Stream.of(labour.getRatePerSheet(), labour.getRatePerEdgeBandingMeter(), labour.getRatePerCabinet(),
				labour.getRatePerHardwareItem(), labour.getRatePerLinearFoot(), labour.getHourlyRate())
				.forEach(rate -> nonNegative(rate, "labour rate"));

		var standard = Optional.ofNullable(settings.getConstructionStandard())
				.orElseThrow(() -> new IllegalArgumentException("a construction standard is required"));
		Map<UUID, Material> byId = catalog.stream().collect(Collectors.toMap(Material::getId, Function.identity()));

		requireKind(byId, standard.getCarcassMaterialId(), MaterialKind.SHEET, "carcass material");
		requireKind(byId, standard.getBackMaterialId(), MaterialKind.SHEET, "back material");
		requireKind(byId, standard.getFrontMaterialId(), MaterialKind.SHEET, "front material");
		requireKind(byId, standard.getCarcassEdgeBandingId(), MaterialKind.EDGE_BANDING, "carcass edge banding");
		requireKind(byId, standard.getFrontEdgeBandingId(), MaterialKind.EDGE_BANDING, "front edge banding");
		requireKind(byId, standard.getOrderedFrontId(), MaterialKind.FRONT, "ordered front style");
		Stream.of(standard.getHingeId(), standard.getShelfPinId(), standard.getLegId(), standard.getPullId(),
				standard.getDrawerSlideId(), standard.getDrawerBoxId())
				.forEach(id -> requireKind(byId, id, MaterialKind.HARDWARE, "hardware"));

		if (standard.getDoorGapMm() < 0 || standard.getToeKickHeightMm() < 0 || standard.getKerfMm() < 0
				|| standard.getSheetTrimMm() < 0) {
			throw new IllegalArgumentException("construction standard dimensions cannot be negative");
		}
	}

	private static void requireKind(Map<UUID, Material> catalog, UUID materialId, MaterialKind kind, String role) {
		var material = Optional.ofNullable(materialId)
				.map(catalog::get)
				.orElseThrow(() -> new IllegalArgumentException("the " + role + " must be in the catalog"));
		if (material.getKind() != kind) {
			throw new IllegalArgumentException("the " + role + " must be of kind " + kind);
		}
	}

	private static void nonNegative(BigDecimal value, String field) {
		if (value == null || value.signum() < 0) {
			throw new IllegalArgumentException("the " + field + " must be zero or more");
		}
	}
}
