package dev.daniellavoie.opencabinet.project.module.adapter.resource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.port.backend.CabinetTemplateRepository;

/**
 * Reads the starter library from {@code cabinet-templates.json}, one entry per Template Version.
 * Versions are only ever added to that file, never edited: {@code CabinetTemplateVersionsTest}
 * fails when a published one changes. The Angular app gets the same templates from the API, so the
 * two sides never hold separate copies.
 */
@Repository
public class CabinetTemplateResourceRepository implements CabinetTemplateRepository {
	private final List<CabinetTemplate> templates;

	public CabinetTemplateResourceRepository(ObjectMapper objectMapper) {
		try (var input = new ClassPathResource("cabinet-templates.json").getInputStream()) {
			this.templates = List.copyOf(objectMapper.readValue(input, new TypeReference<List<CabinetTemplate>>() {
			}));
		} catch (IOException exception) {
			throw new UncheckedIOException("could not read cabinet-templates.json", exception);
		}
	}

	@Override
	public List<CabinetTemplate> findAll() {
		return templates;
	}
}
