package dev.daniellavoie.opencabinet.cutlist.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.daniellavoie.opencabinet.project.domain.model.PartRole;
import dev.daniellavoie.opencabinet.cutlist.domain.model.SheetPlacement;

/**
 * The Angular app reads these exact names. A field like {@code xMm} would not survive: Lombok's
 * {@code getXMm()} is published by Jackson as {@code xmm}.
 */
class SheetPlacementJsonTest {
	@Test
	void coordinatesKeepTheirNames() throws Exception {
		var placement = SheetPlacement.builder().cabinetLabel("C1").role(PartRole.SIDE).alongMm(10).acrossMm(20)
				.lengthMm(300).widthMm(200).build();

		var json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(placement));

		assertThat(json.has("alongMm")).isTrue();
		assertThat(json.has("acrossMm")).isTrue();
		assertThat(new ObjectMapper().treeToValue(json, SheetPlacement.class)).isEqualTo(placement);
	}
}
