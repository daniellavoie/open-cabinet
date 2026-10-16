package dev.daniellavoie.opencabinet.project.domain.port.backend;

import java.util.List;

import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;

public interface CabinetTemplateRepository {
	/**
	 * The starter library of Cabinet Templates that ships with Open Cabinet.
	 */
	List<CabinetTemplate> findAll();
}
