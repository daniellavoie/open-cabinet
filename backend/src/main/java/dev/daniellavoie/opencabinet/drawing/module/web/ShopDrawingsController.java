package dev.daniellavoie.opencabinet.drawing.module.web;

import java.security.Principal;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.daniellavoie.opencabinet.drawing.domain.model.DrawingLanguage;
import dev.daniellavoie.opencabinet.drawing.domain.port.api.ShopDrawingsDomain;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@AllArgsConstructor
@RequestMapping("/api/project/{projectId}/shop-drawings")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShopDrawingsController {
	ShopDrawingsDomain domain;

	/**
	 * The Shop Drawings as a PDF, in {@code language} (fr or en).
	 */
	@GetMapping("/pdf")
	public ResponseEntity<byte[]> pdf(@PathVariable UUID projectId,
			@RequestParam(defaultValue = "fr") String language, Principal principal) {
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.body(domain.pdf(projectId, DrawingLanguage.of(language), principal));
	}
}
