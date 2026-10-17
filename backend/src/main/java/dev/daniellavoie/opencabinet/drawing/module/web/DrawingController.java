package dev.daniellavoie.opencabinet.drawing.module.web;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.daniellavoie.opencabinet.common.domain.Language;
import dev.daniellavoie.opencabinet.drawing.domain.model.ShopDrawingSheet;
import dev.daniellavoie.opencabinet.drawing.domain.port.api.DrawingDomain;
import dev.daniellavoie.opencabinet.drawing.module.adapter.svg.SvgCanvas;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@AllArgsConstructor
@RequestMapping("/api/project/{projectId}/drawing")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DrawingController {
	DrawingDomain domain;

	/**
	 * A drawing ready to show: what it shows, its title and an SVG document.
	 */
	public record DrawingView(ShopDrawingSheet.Kind kind, String title, String svg) {
	}

	@GetMapping
	public List<DrawingView> drawings(@PathVariable UUID projectId,
			@RequestParam(name = "lang", required = false) String language, Principal principal) {
		return domain.shopDrawings(projectId, Language.of(language), principal).sheets().stream()
				.map(sheet -> new DrawingView(sheet.kind(), sheet.drawing().title(), SvgCanvas.render(sheet.drawing())))
				.toList();
	}

	@GetMapping("/pdf")
	public ResponseEntity<byte[]> pdf(@PathVariable UUID projectId,
			@RequestParam(name = "lang", required = false) String language, Principal principal) {
		var lang = Language.of(language);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(lang.pick("dessins-atelier.pdf", "shop-drawings.pdf"))
						.build()
						.toString())
				.body(domain.shopDrawingsPdf(projectId, lang, principal));
	}
}
