package dev.daniellavoie.opencabinet.template.domain.model;

import java.util.UUID;

import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A Cabinet Template a Shop saved from a designed Cabinet: a named preset of a starter template
 * ({@code baseCode}). Cabinets added from it keep the base template's code, so the Construction
 * Rules and Parts follow the base template. The preset holds no id, Wall or position.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class ShopCabinetTemplate {
	UUID id;
	String name;
	String baseCode;
	Cabinet preset;
}
