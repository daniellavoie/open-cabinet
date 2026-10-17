package dev.daniellavoie.opencabinet.project.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.daniellavoie.opencabinet.project.domain.FrontLayout.Row;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.FrontKind;
import dev.daniellavoie.opencabinet.project.domain.model.FrontRow;

class FrontLayoutTest {
	@Test
	void fixedRowsKeepTheirHeightAndFlexibleRowsShareTheRestByWeight() {
		var template = CabinetTemplate.builder()
				.frontRows(List.of(
						FrontRow.builder().kind(FrontKind.DRAWER).heightMm(150.0).build(),
						FrontRow.builder().kind(FrontKind.DOOR).weight(1).build(),
						FrontRow.builder().kind(FrontKind.DOOR).weight(3).build()))
				.build();

		var rows = FrontLayout.rows(template, 1009, 3);

		// 1009 - 3 gaps of 3 = 1000; 150 fixed, 850 shared 1:3
		assertThat(rows).extracting(Row::heightMm).containsExactly(150.0, 212.5, 637.5);
		assertThat(rows.get(0).bottomMm()).isCloseTo(1009 - 1.5 - 150, within(1e-9));
		assertThat(rows.get(2).bottomMm()).isCloseTo(1.5, within(1e-9));
	}

	@Test
	void rowsLeftWithoutHeightAreDropped() {
		var template = CabinetTemplate.builder()
				.frontRows(List.of(
						FrontRow.builder().kind(FrontKind.OPENING).heightMm(800.0).build(),
						FrontRow.builder().kind(FrontKind.DRAWER).build()))
				.build();

		assertThat(FrontLayout.rows(template, 700, 3)).extracting(Row::kind).containsExactly(FrontKind.OPENING);
	}
}
