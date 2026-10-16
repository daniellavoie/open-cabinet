package dev.daniellavoie.opencabinet.quote.module.web;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.daniellavoie.opencabinet.quote.domain.model.Quote;
import dev.daniellavoie.opencabinet.quote.domain.model.QuoteStatusRequest;
import dev.daniellavoie.opencabinet.quote.domain.port.api.QuoteDomain;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@AllArgsConstructor
@RequestMapping("/api")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuoteController {
	QuoteDomain domain;

	@GetMapping("/project/{projectId}/quote")
	public List<Quote> findByProject(@PathVariable UUID projectId, Principal principal) {
		return domain.findByProject(projectId, principal);
	}

	@PostMapping("/project/{projectId}/quote")
	public Quote create(@PathVariable UUID projectId, Principal principal) {
		return domain.create(projectId, principal);
	}

	@GetMapping("/quote/{quoteId}")
	public Quote findById(@PathVariable UUID quoteId, Principal principal) {
		return domain.findById(quoteId, principal);
	}

	@PostMapping("/quote/{quoteId}/send")
	public Quote send(@PathVariable UUID quoteId, Principal principal) {
		return domain.send(quoteId, principal);
	}

	@PutMapping("/quote/{quoteId}/status")
	public Quote updateStatus(@PathVariable UUID quoteId, @RequestBody QuoteStatusRequest request,
			Principal principal) {
		return domain.updateStatus(quoteId, request.getStatus(), principal);
	}

	@DeleteMapping("/quote/{quoteId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID quoteId, Principal principal) {
		domain.delete(quoteId, principal);
	}

	@GetMapping("/quote/{quoteId}/pdf")
	public ResponseEntity<byte[]> pdf(@PathVariable UUID quoteId, Principal principal) {
		var quote = domain.findById(quoteId, principal);

		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(quote.getDisplayNumber() + ".pdf")
						.build()
						.toString())
				.body(domain.pdf(quoteId, principal));
	}
}
