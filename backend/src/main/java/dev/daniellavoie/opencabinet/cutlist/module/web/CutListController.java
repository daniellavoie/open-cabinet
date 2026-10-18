package dev.daniellavoie.opencabinet.cutlist.module.web;

import java.nio.charset.StandardCharsets;
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

import dev.daniellavoie.opencabinet.cutlist.domain.model.OutputDocument;
import dev.daniellavoie.opencabinet.cutlist.domain.model.ProjectOutput;
import dev.daniellavoie.opencabinet.cutlist.domain.port.api.CutListDomain;
import dev.daniellavoie.opencabinet.cutlist.domain.port.api.OutputDocumentDomain;
import dev.daniellavoie.opencabinet.drawing.domain.model.DrawingLanguage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@AllArgsConstructor
@RequestMapping("/api/project/{projectId}/output")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CutListController {
	CutListDomain domain;
	OutputDocumentDomain documentDomain;

	@GetMapping
	public ProjectOutput output(@PathVariable UUID projectId, Principal principal) {
		return domain.output(projectId, principal);
	}

	/**
	 * The cut-list, nesting or bill-of-materials as a PDF, in {@code language} (fr or en).
	 */
	@GetMapping("/{document}/pdf")
	public ResponseEntity<byte[]> pdf(@PathVariable UUID projectId, @PathVariable String document,
			@RequestParam(defaultValue = "fr") String language, Principal principal) {
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.body(documentDomain.pdf(projectId, OutputDocument.of(document), DrawingLanguage.of(language),
						principal));
	}

	/**
	 * The same as CSV.
	 */
	@GetMapping("/{document}/csv")
	public ResponseEntity<String> csv(@PathVariable UUID projectId, @PathVariable String document,
			@RequestParam(defaultValue = "fr") String language, Principal principal) {
		return ResponseEntity.ok()
				.contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.body(documentDomain.csv(projectId, OutputDocument.of(document), DrawingLanguage.of(language),
						principal));
	}
}
