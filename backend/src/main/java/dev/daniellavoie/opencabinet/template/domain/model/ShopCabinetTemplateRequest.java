package dev.daniellavoie.opencabinet.template.domain.model;

import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class ShopCabinetTemplateRequest {
	String name;
	Cabinet cabinet;
}
