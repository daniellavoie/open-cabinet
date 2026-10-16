package dev.daniellavoie.opencabinet.catalog.domain;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.catalog.domain.model.MaterialKind;
import dev.daniellavoie.opencabinet.catalog.domain.model.MaterialRequest;
import dev.daniellavoie.opencabinet.catalog.domain.port.api.CatalogDomain;
import dev.daniellavoie.opencabinet.catalog.domain.port.backend.MaterialRepository;
import dev.daniellavoie.opencabinet.member.domain.port.api.MemberDomain;
import dev.daniellavoie.opencabinet.shop.domain.model.ConstructionStandard;
import dev.daniellavoie.opencabinet.shop.domain.port.backend.ShopRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultCatalogDomain implements CatalogDomain {
	private static final double INCH = 25.4;

	MaterialRepository repository;
	MemberDomain memberDomain;
	ShopRepository shopRepository;

	@Override
	public List<Material> findAll(Principal principal) {
		return repository.findByShop(memberDomain.current(principal).getShopId());
	}

	@Override
	public List<Material> findByShop(UUID shopId) {
		return repository.findByShop(shopId);
	}

	@Override
	public Material create(MaterialRequest request, Principal principal) {
		var owner = memberDomain.currentOwner(principal);

		return repository.save(toMaterial(UUID.randomUUID(), request), owner.getShopId());
	}

	@Override
	public Material update(UUID materialId, MaterialRequest request, Principal principal) {
		var owner = memberDomain.currentOwner(principal);
		var existing = repository.findById(materialId, owner.getShopId());

		if (existing.getKind() != request.getKind()) {
			throw new IllegalArgumentException("the kind of a material cannot change");
		}

		return repository.save(toMaterial(materialId, request), owner.getShopId());
	}

	@Override
	public void delete(UUID materialId, Principal principal) {
		var owner = memberDomain.currentOwner(principal);
		var standard = shopRepository.findById(owner.getShopId()).getSettings().getConstructionStandard();

		if (usedIds(standard).anyMatch(materialId::equals)) {
			throw new IllegalStateException("this material is used by the construction standard");
		}

		repository.delete(materialId, owner.getShopId());
	}

	@Override
	public ConstructionStandard seedDefaults(UUID shopId) {
		var carcass = seed(shopId, sheet("Mélamine blanche 5/8\"", "MEL-WH-58", "68.00", 5 / 8d, 97, 49, false));
		var back = seed(shopId, sheet("Mélamine blanche 1/4\" (dos)", "MEL-WH-14", "38.00", 1 / 4d, 96, 48, false));
		var front = seed(shopId,
				sheet("Mélamine texturée noyer 5/8\"", "MEL-WAL-58", "95.00", 5 / 8d, 97, 49, true));
		var carcassBanding = seed(shopId, edgeBanding("Bande de chant PVC blanche 15/16\"", "EB-WH", "0.45"));
		var frontBanding = seed(shopId, edgeBanding("Bande de chant PVC noyer 15/16\"", "EB-WAL", "0.85"));
		var hinge = seed(shopId, hardware("Charnière à fermeture douce 110°", "HNG-110", "6.50"));
		var shelfPin = seed(shopId, hardware("Taquet de tablette 5 mm", "PIN-5", "0.15"));
		var leg = seed(shopId, hardware("Patte ajustable", "LEG-ADJ", "2.20"));
		var pull = seed(shopId, hardware("Poignée", "PULL-128", "8.00"));
		var slide = seed(shopId, hardware("Coulisse à fermeture douce (paire)", "SLD-21", "48.00"));
		var drawerBox = seed(shopId, hardware("Boîte de tiroir", "DRW-BOX", "45.00"));

		return ConstructionStandard.builder()
				.carcassMaterialId(carcass.getId())
				.backMaterialId(back.getId())
				.frontMaterialId(front.getId())
				.carcassEdgeBandingId(carcassBanding.getId())
				.frontEdgeBandingId(frontBanding.getId())
				.hingeId(hinge.getId())
				.shelfPinId(shelfPin.getId())
				.legId(leg.getId())
				.pullId(pull.getId())
				.drawerSlideId(slide.getId())
				.drawerBoxId(drawerBox.getId())
				.doorGapMm(3)
				.toeKickHeightMm(4 * INCH)
				.kerfMm(4)
				.sheetTrimMm(10)
				.build();
	}

	private Material seed(UUID shopId, MaterialRequest request) {
		return repository.save(toMaterial(UUID.randomUUID(), request), shopId);
	}

	private static MaterialRequest sheet(String name, String sku, String price, double thicknessInches,
			double lengthInches, double widthInches, boolean grain) {
		return MaterialRequest.builder()
				.kind(MaterialKind.SHEET)
				.name(name)
				.sku(sku)
				.price(new BigDecimal(price))
				.thicknessMm(round(thicknessInches * INCH))
				.lengthMm(round(lengthInches * INCH))
				.widthMm(round(widthInches * INCH))
				.grain(grain)
				.build();
	}

	private static MaterialRequest edgeBanding(String name, String sku, String pricePerMeter) {
		return MaterialRequest.builder()
				.kind(MaterialKind.EDGE_BANDING)
				.name(name)
				.sku(sku)
				.price(new BigDecimal(pricePerMeter))
				.build();
	}

	private static MaterialRequest hardware(String name, String sku, String price) {
		return MaterialRequest.builder()
				.kind(MaterialKind.HARDWARE)
				.name(name)
				.sku(sku)
				.price(new BigDecimal(price))
				.build();
	}

	private static double round(double millimetres) {
		return Math.round(millimetres * 10) / 10d;
	}

	private static Material toMaterial(UUID materialId, MaterialRequest request) {
		var kind = Optional.ofNullable(request.getKind())
				.orElseThrow(() -> new IllegalArgumentException("a material kind is required"));
		var name = Optional.ofNullable(request.getName())
				.filter(value -> !value.isBlank())
				.orElseThrow(() -> new IllegalArgumentException("a material name is required"));
		var price = Optional.ofNullable(request.getPrice())
				.filter(value -> value.signum() >= 0)
				.orElseThrow(() -> new IllegalArgumentException("a price of zero or more is required"));

		if (kind == MaterialKind.SHEET && Stream.of(request.getThicknessMm(), request.getLengthMm(),
				request.getWidthMm()).anyMatch(value -> value == null || value <= 0)) {
			throw new IllegalArgumentException("a sheet needs a thickness, length and width");
		}

		return Material.builder()
				.id(materialId)
				.kind(kind)
				.name(name)
				.sku(request.getSku())
				.price(price)
				.thicknessMm(kind == MaterialKind.SHEET ? request.getThicknessMm() : null)
				.lengthMm(kind == MaterialKind.SHEET ? request.getLengthMm() : null)
				.widthMm(kind == MaterialKind.SHEET ? request.getWidthMm() : null)
				.grain(kind == MaterialKind.SHEET && request.isGrain())
				.build();
	}

	private static Stream<UUID> usedIds(ConstructionStandard standard) {
		return Stream.of(standard.getCarcassMaterialId(), standard.getBackMaterialId(),
				standard.getFrontMaterialId(), standard.getCarcassEdgeBandingId(), standard.getFrontEdgeBandingId(),
				standard.getHingeId(), standard.getShelfPinId(), standard.getLegId(), standard.getPullId(),
				standard.getDrawerSlideId(), standard.getDrawerBoxId());
	}
}
