package dev.daniellavoie.opencabinet.cutlist.module.web;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
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
import dev.daniellavoie.opencabinet.cutlist.domain.model.ProjectOutput;
import dev.daniellavoie.opencabinet.cutlist.domain.port.api.CutListDomain;
import dev.daniellavoie.opencabinet.cutlist.module.adapter.export.OutputCsv;
import dev.daniellavoie.opencabinet.cutlist.module.adapter.export.OutputKind;
import dev.daniellavoie.opencabinet.cutlist.module.adapter.export.OutputPdf;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@AllArgsConstructor
@RequestMapping("/api/project/{projectId}/output")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CutListController {
	CutListDomain domain;

	@GetMapping
	public ProjectOutput output(@PathVariable UUID projectId, Principal principal) {
		return domain.output(projectId, principal);
	}

	@GetMapping("/{kind}/csv")
	public ResponseEntity<byte[]> csv(@PathVariable UUID projectId, @PathVariable String kind,
			@RequestParam(name = "lang", required = false) String language, Principal principal) {
		var outputKind = OutputKind.of(kind);
		var document = domain.document(projectId, Language.of(language), principal);
		return file(new MediaType("text", "csv", StandardCharsets.UTF_8),
				outputKind.fileName(document.language()) + ".csv", OutputCsv.write(document, outputKind));
	}

	@GetMapping("/{kind}/pdf")
	public ResponseEntity<byte[]> pdf(@PathVariable UUID projectId, @PathVariable String kind,
			@RequestParam(name = "lang", required = false) String language, Principal principal) {
		var outputKind = OutputKind.of(kind);
		var document = domain.document(projectId, Language.of(language), principal);
		return file(MediaType.APPLICATION_PDF, outputKind.fileName(document.language()) + ".pdf",
				OutputPdf.write(document, outputKind));
	}

	private static ResponseEntity<byte[]> file(MediaType type, String name, byte[] body) {
		return ResponseEntity.ok()
				.contentType(type)
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build().toString())
				.body(body);
	}
}
