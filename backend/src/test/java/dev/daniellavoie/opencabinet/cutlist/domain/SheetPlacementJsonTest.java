package dev.daniellavoie.opencabinet.cutlist.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import dev.daniellavoie.opencabinet.common.config.JsonConfiguration;
import dev.daniellavoie.opencabinet.cutlist.domain.model.SheetPlacement;
import dev.daniellavoie.opencabinet.project.domain.model.PartRole;

/**
 * The Angular app reads these exact names. A field like {@code xMm} would not survive: Lombok's
 * {@code getXMm()} is published by Jackson as {@code xmm}.
 */
class SheetPlacementJsonTest {
	@Test
	void coordinatesKeepTheirNames() throws Exception {
		var placement = SheetPlacement.builder().cabinetLabel("C1").role(PartRole.SIDE).alongMm(10).acrossMm(20)
				.lengthMm(300).widthMm(200).build();

		var json = JsonConfiguration.configure(JsonMapper.builder()).build().readTree(JsonConfiguration.configure(JsonMapper.builder()).build().writeValueAsString(placement));

		assertThat(json.has("alongMm")).isTrue();
		assertThat(json.has("acrossMm")).isTrue();
		assertThat(JsonConfiguration.configure(JsonMapper.builder()).build().treeToValue(json, SheetPlacement.class)).isEqualTo(placement);
	}
}
