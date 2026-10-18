package dev.daniellavoie.opencabinet.project.module.adapter.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Published Template Versions never change (docs/adr/0005-cabinet-keeps-its-template-version.md).
 * {@code cabinet-templates.published} lists every starter Template Version with a hash of its
 * content: editing a published version fails here, and a new version must be added to that list.
 */
class CabinetTemplateVersionsTest {
	private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
			.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
			.build();

	@Test
	void publishedVersionsNeverChange() throws IOException {
		var hashes = versions().entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> hash(entry.getValue())));

		for (var line : published()) {
			var parts = line.split(" ");
			assertThat(hashes)
					.as("Template Version %s was published and must never change or disappear: "
							+ "publish a new version instead", parts[0])
					.containsEntry(parts[0], parts[1]);
		}
	}

	@Test
	void everyVersionIsPublished() throws IOException {
		var published = published().stream().map(line -> line.split(" ")[0]).toList();
		var unlisted = versions().entrySet().stream()
				.filter(entry -> !published.contains(entry.getKey()))
				.map(entry -> entry.getKey() + " " + hash(entry.getValue()))
				.toList();

		assertThat(unlisted)
				.as("add these lines to cabinet-templates.published")
				.isEmpty();
	}

	@Test
	void versionsCountFromOneWithChangeNotes() throws IOException {
		var byCode = new LinkedHashMap<String, List<Map<String, Object>>>();
		for (var version : versions().values()) {
			byCode.computeIfAbsent((String) version.get("code"), code -> new ArrayList<>()).add(version);
		}

		byCode.forEach((code, versions) -> {
			for (int index = 0; index < versions.size(); index++) {
				var version = versions.get(index);
				assertThat(version.get("version")).as("%s versions in order from 1", code).isEqualTo(index + 1);
				if (index > 0) {
					assertThat((String) version.get("changeNote")).as("%s@%d change note", code, index + 1)
							.isNotBlank();
				}
			}
		});
	}

	/**
	 * Every Template Version in {@code cabinet-templates.json}, by code@version, in file order.
	 */
	private static Map<String, Map<String, Object>> versions() throws IOException {
		try (var input = new ClassPathResource("cabinet-templates.json").getInputStream()) {
			List<Map<String, Object>> entries = OBJECT_MAPPER.readValue(input,
					new TypeReference<List<Map<String, Object>>>() {
					});
			var versions = new LinkedHashMap<String, Map<String, Object>>();
			for (var entry : entries) {
				var key = entry.get("code") + "@" + entry.get("version");
				assertThat(versions.put(key, entry)).as("%s appears once", key).isNull();
			}
			return versions;
		}
	}

	private static List<String> published() throws IOException {
		try (InputStream input = new ClassPathResource("cabinet-templates.published").getInputStream()) {
			return new String(input.readAllBytes(), StandardCharsets.UTF_8).lines()
					.map(String::trim)
					.filter(line -> !line.isEmpty() && !line.startsWith("#"))
					.toList();
		}
	}

	/**
	 * Hashes the content, not the formatting: keys sorted, no whitespace.
	 */
	private static String hash(Map<String, Object> version) {
		try {
			var canonical = OBJECT_MAPPER.writeValueAsString(version);
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(canonical.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
