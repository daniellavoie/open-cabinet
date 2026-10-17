package dev.daniellavoie.opencabinet.project.domain.port.backend;

import java.util.List;

import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;

public interface CabinetTemplateRepository {
	/**
	 * Every published Template Version of the starter library that ships with Open Cabinet.
	 */
	List<CabinetTemplate> findAll();
}
