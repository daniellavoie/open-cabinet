package dev.daniellavoie.opencabinet.template.domain.model;

import dev.daniellavoie.opencabinet.project.domain.model.CabinetType;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Starts a template from a Template Version ({@code templateCode} and {@code templateVersion}) or
 * from one of the Project's Cabinets ({@code cabinetId}), whose size becomes its default. In the
 * Shop library, a {@code type} alone starts from scratch: no Parts, that type's usual sizes.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class CreateProjectTemplateRequest {
	String name;
	String templateCode;
	Integer templateVersion;
	String cabinetId;
	CabinetType type;
}
