package dev.daniellavoie.opencabinet.cutlist.domain.port.api;

import java.security.Principal;
import java.util.UUID;

import dev.daniellavoie.opencabinet.cutlist.domain.model.OutputDocument;
import dev.daniellavoie.opencabinet.drawing.domain.model.DrawingLanguage;

/**
 * A saved Project's Cut List, Nesting or Bill of Materials, to print or to open in a spreadsheet.
 */
public interface OutputDocumentDomain {
	byte[] pdf(UUID projectId, OutputDocument document, DrawingLanguage language, Principal principal);

	String csv(UUID projectId, OutputDocument document, DrawingLanguage language, Principal principal);
}
