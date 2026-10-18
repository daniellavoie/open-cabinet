package dev.daniellavoie.opencabinet.cutlist.domain;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dev.daniellavoie.opencabinet.catalog.domain.port.api.CatalogDomain;
import dev.daniellavoie.opencabinet.client.domain.port.api.ClientDomain;
import dev.daniellavoie.opencabinet.cutlist.domain.model.OutputDocument;
import dev.daniellavoie.opencabinet.cutlist.domain.model.PrintedOutput;
import dev.daniellavoie.opencabinet.cutlist.domain.port.api.CutListDomain;
import dev.daniellavoie.opencabinet.cutlist.domain.port.api.OutputDocumentDomain;
import dev.daniellavoie.opencabinet.cutlist.domain.port.backend.ProjectOutputRenderer;
import dev.daniellavoie.opencabinet.drawing.domain.model.DrawingLanguage;
import dev.daniellavoie.opencabinet.member.domain.port.api.MemberDomain;
import dev.daniellavoie.opencabinet.project.domain.port.api.ProjectDomain;
import dev.daniellavoie.opencabinet.shop.domain.port.backend.ShopRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultOutputDocumentDomain implements OutputDocumentDomain {
	MemberDomain memberDomain;
	ProjectDomain projectDomain;
	ClientDomain clientDomain;
	CatalogDomain catalogDomain;
	CutListDomain cutListDomain;
	ShopRepository shopRepository;
	ProjectOutputRenderer renderer;

	@Override
	public byte[] pdf(UUID projectId, OutputDocument document, DrawingLanguage language, Principal principal) {
		return renderer.pdf(printed(projectId, document, language, principal));
	}

	@Override
	public String csv(UUID projectId, OutputDocument document, DrawingLanguage language, Principal principal) {
		var printed = printed(projectId, document, language, principal);
		return ProjectOutputCsv.write(document, printed.getOutput(), printed.getUnits(), language);
	}

	private PrintedOutput printed(UUID projectId, OutputDocument document, DrawingLanguage language,
			Principal principal) {
		var shopId = memberDomain.current(principal).getShopId();
		var shop = shopRepository.findById(shopId);
		var project = projectDomain.findById(projectId, shopId);
		var output = cutListDomain.output(project, shop.getSettings().getConstructionStandard(),
				catalogDomain.findByShop(shopId));

		return PrintedOutput.builder()
				.document(document)
				.shopName(shop.getName())
				.projectName(project.getName())
				.clientName(clientDomain.findById(project.getClientId(), shopId).getName())
				.date(LocalDate.now())
				.units(shop.getUnits())
				.currency(shop.getCurrency())
				.language(language)
				.output(output)
				.sheets(document == OutputDocument.NESTING
						? NestingDrawings.sheets(output.getNesting(), shop.getUnits(), language)
						: List.of())
				.build();
	}
}
