package dev.daniellavoie.opencabinet.assistant.module.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.daniellavoie.opencabinet.assistant.domain.model.AcceptResult;
import dev.daniellavoie.opencabinet.assistant.domain.model.ConversationView;
import dev.daniellavoie.opencabinet.assistant.domain.model.DesignProposal;
import dev.daniellavoie.opencabinet.assistant.domain.model.SendMessageRequest;
import dev.daniellavoie.opencabinet.assistant.domain.model.UploadedFile;
import dev.daniellavoie.opencabinet.assistant.domain.port.api.ConversationDomain;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@AllArgsConstructor
@RequestMapping("/api/project/{projectId}")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationController {
	ConversationDomain domain;

	@GetMapping("/conversation")
	public ConversationView view(@PathVariable UUID projectId, @RequestParam(defaultValue = "0") int afterProgress,
			Principal principal) {
		return domain.view(projectId, afterProgress, principal);
	}

	@PostMapping(path = "/conversation/message", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ConversationView send(@PathVariable UUID projectId, @RequestBody SendMessageRequest request, Principal principal) {
		return domain.send(projectId, request, List.of(), principal);
	}

	/**
	 * A message with photos, sketches or PDFs attached.
	 */
	@PostMapping(path = "/conversation/message", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ConversationView sendWithFiles(@PathVariable UUID projectId, @RequestParam String text,
			@RequestParam(required = false) String language, @RequestParam(name = "files", required = false) List<MultipartFile> files,
			Principal principal) throws IOException {
		var uploaded = new ArrayList<UploadedFile>();
		for (var file : Optional.ofNullable(files).orElse(List.of())) {
			uploaded.add(new UploadedFile(file.getOriginalFilename(), file.getBytes()));
		}
		return domain.send(projectId, SendMessageRequest.builder().text(text).language(language).build(), uploaded, principal);
	}

	@GetMapping("/conversation/attachment/{attachmentId}")
	public ResponseEntity<byte[]> attachment(@PathVariable UUID projectId, @PathVariable UUID attachmentId, Principal principal) {
		var content = domain.attachment(projectId, attachmentId, principal);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(content.mediaType()))
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
						.filename(content.fileName(), StandardCharsets.UTF_8)
						.build()
						.toString())
				.header("X-Content-Type-Options", "nosniff")
				.cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
				.body(content.bytes());
	}

	@PostMapping("/proposal/{proposalId}/accept")
	public AcceptResult accept(@PathVariable UUID projectId, @PathVariable UUID proposalId, Principal principal) {
		return domain.accept(projectId, proposalId, principal);
	}

	@PostMapping("/proposal/{proposalId}/undo")
	public AcceptResult undoAccept(@PathVariable UUID projectId, @PathVariable UUID proposalId, Principal principal) {
		return domain.undoAccept(projectId, proposalId, principal);
	}

	@PostMapping("/proposal/{proposalId}/discard")
	public DesignProposal discard(@PathVariable UUID projectId, @PathVariable UUID proposalId, Principal principal) {
		return domain.discard(projectId, proposalId, principal);
	}
}
