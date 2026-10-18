package dev.daniellavoie.opencabinet.assistant.domain;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import dev.daniellavoie.opencabinet.assistant.domain.model.AttachmentContent;
import dev.daniellavoie.opencabinet.assistant.domain.model.UploadedFile;
import dev.daniellavoie.opencabinet.assistant.domain.port.backend.PdfInspector;

/**
 * What a Member may attach to a message: photos and sketches (JPEG, PNG, WebP, GIF) and PDFs, the
 * type read from the bytes rather than the name. The limits are the model provider's, checked here
 * so a Member hears about them at once rather than when the assistant fails.
 */
final class Attachments {
	static final int MAX_FILES = 10;
	static final long MAX_MESSAGE_BYTES = 25L * 1024 * 1024;
	static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
	static final int MAX_PDF_PAGES = 100;
	static final int MAX_NAME_LENGTH = 255;

	private Attachments() {
	}

	/**
	 * The files, with their detected types; rejects any that cannot be sent with an
	 * {@link IllegalArgumentException}.
	 */
	static List<AttachmentContent> check(List<UploadedFile> files, PdfInspector pdfs) {
		if (files.size() > MAX_FILES) {
			throw new IllegalArgumentException("a message takes at most " + MAX_FILES + " attachments");
		}
		var total = files.stream().mapToLong(file -> file.bytes().length).sum();
		if (total > MAX_MESSAGE_BYTES) {
			throw new IllegalArgumentException("a message's attachments take at most 25 MB");
		}
		return files.stream().map(file -> check(file, pdfs)).toList();
	}

	private static AttachmentContent check(UploadedFile file, PdfInspector pdfs) {
		var name = name(file.fileName());
		var bytes = file.bytes();
		if (bytes.length == 0) {
			throw new IllegalArgumentException(name + " is empty");
		}
		var type = detect(bytes)
				.orElseThrow(() -> new IllegalArgumentException(name + " is not a photo (JPEG, PNG, WebP, GIF) or a PDF"));
		if (type.startsWith("image/") && bytes.length > MAX_IMAGE_BYTES) {
			throw new IllegalArgumentException(name + " is larger than 5 MB");
		}
		if (type.equals("application/pdf") && pdfs.pageCount(bytes) > MAX_PDF_PAGES) {
			throw new IllegalArgumentException(name + " has more than " + MAX_PDF_PAGES + " pages");
		}
		return new AttachmentContent(name, type, bytes);
	}

	static Optional<String> detect(byte[] bytes) {
		if (startsWith(bytes, 0, 0xFF, 0xD8, 0xFF)) {
			return Optional.of("image/jpeg");
		}
		if (startsWith(bytes, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
			return Optional.of("image/png");
		}
		if (ascii(bytes, 0, "GIF87a") || ascii(bytes, 0, "GIF89a")) {
			return Optional.of("image/gif");
		}
		if (ascii(bytes, 0, "RIFF") && ascii(bytes, 8, "WEBP")) {
			return Optional.of("image/webp");
		}
		if (ascii(bytes, 0, "%PDF-")) {
			return Optional.of("application/pdf");
		}
		return Optional.empty();
	}

	/**
	 * The name without any path, at most 255 characters.
	 */
	static String name(String fileName) {
		var name = Optional.ofNullable(fileName)
				.map(value -> value.substring(Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\')) + 1).strip())
				.filter(value -> !value.isEmpty())
				.orElse("attachment");
		return name.length() > MAX_NAME_LENGTH ? name.substring(name.length() - MAX_NAME_LENGTH) : name;
	}

	private static boolean startsWith(byte[] bytes, int offset, int... expected) {
		if (bytes.length < offset + expected.length) {
			return false;
		}
		for (var index = 0; index < expected.length; index++) {
			if ((bytes[offset + index] & 0xFF) != expected[index]) {
				return false;
			}
		}
		return true;
	}

	private static boolean ascii(byte[] bytes, int offset, String expected) {
		var wanted = expected.getBytes(StandardCharsets.US_ASCII);
		return bytes.length >= offset + wanted.length
				&& Arrays.equals(Arrays.copyOfRange(bytes, offset, offset + wanted.length), wanted);
	}
}
