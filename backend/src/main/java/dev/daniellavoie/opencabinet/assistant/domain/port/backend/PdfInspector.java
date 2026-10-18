package dev.daniellavoie.opencabinet.assistant.domain.port.backend;

/**
 * Reads what the assistant needs to know about a PDF before sending it to the model.
 */
public interface PdfInspector {
	/**
	 * The PDF's page count; rejects a file that is not a readable PDF with an
	 * {@link IllegalArgumentException}.
	 */
	int pageCount(byte[] pdf);
}
