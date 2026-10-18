package dev.daniellavoie.opencabinet.assistant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.daniellavoie.opencabinet.assistant.domain.model.AttachmentContent;
import dev.daniellavoie.opencabinet.assistant.domain.model.UploadedFile;

class AttachmentsTest {
	private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0 };
	private static final byte[] PDF = "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII);

	@Test
	void readsTheTypeFromTheBytesNotTheName() {
		assertThat(Attachments.detect(JPEG)).contains("image/jpeg");
		assertThat(Attachments.detect(new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A })).contains("image/png");
		assertThat(Attachments.detect("GIF89a...".getBytes(StandardCharsets.US_ASCII))).contains("image/gif");
		assertThat(Attachments.detect("RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.US_ASCII))).contains("image/webp");
		assertThat(Attachments.detect(PDF)).contains("application/pdf");
		assertThat(Attachments.detect("<svg>".getBytes(StandardCharsets.US_ASCII))).isEmpty();

		var checked = Attachments.check(List.of(new UploadedFile("plan.png", PDF)), pdf -> 3);
		assertThat(checked).extracting(AttachmentContent::mediaType).containsExactly("application/pdf");
	}

	@Test
	void keepsOnlyTheFileName() {
		assertThat(Attachments.name("C:\\Users\\marie\\cuisine.jpg")).isEqualTo("cuisine.jpg");
		assertThat(Attachments.name("../../etc/passwd")).isEqualTo("passwd");
		assertThat(Attachments.name("  ")).isEqualTo("attachment");
		assertThat(Attachments.name("a".repeat(300) + ".pdf")).hasSize(255).endsWith(".pdf");
	}

	@Test
	void refusesWhatTheModelCannotTake() {
		assertThatThrownBy(() -> Attachments.check(List.of(new UploadedFile("vide.jpg", new byte[0])), pdf -> 1))
				.hasMessageContaining("empty");
		assertThatThrownBy(() -> Attachments.check(Collections.nCopies(11, new UploadedFile("a.jpg", JPEG)), pdf -> 1))
				.hasMessageContaining("10");
		var large = new byte[(int) Attachments.MAX_IMAGE_BYTES + 1];
		System.arraycopy(JPEG, 0, large, 0, JPEG.length);
		assertThatThrownBy(() -> Attachments.check(List.of(new UploadedFile("grande.jpg", large)), pdf -> 1))
				.hasMessageContaining("5 MB");
		assertThatThrownBy(() -> Attachments.check(List.of(new UploadedFile("long.pdf", PDF)), pdf -> 101))
				.hasMessageContaining("100 pages");
		var half = new byte[(int) (Attachments.MAX_MESSAGE_BYTES / 2) + 1];
		System.arraycopy(PDF, 0, half, 0, PDF.length);
		assertThatThrownBy(() -> Attachments.check(List.of(new UploadedFile("a.pdf", half), new UploadedFile("b.pdf", half)),
				pdf -> 1)).hasMessageContaining("25 MB");
	}
}
