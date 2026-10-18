package dev.daniellavoie.opencabinet.assistant.module.adapter.pdf;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.lowagie.text.pdf.PdfReader;

import dev.daniellavoie.opencabinet.assistant.domain.port.backend.PdfInspector;

@Component
public class OpenPdfInspector implements PdfInspector {
	@Override
	public int pageCount(byte[] pdf) {
		try (var reader = new PdfReader(pdf)) {
			return reader.getNumberOfPages();
		} catch (IOException | RuntimeException exception) {
			throw new IllegalArgumentException("the PDF cannot be read");
		}
	}
}
