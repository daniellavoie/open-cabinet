package dev.daniellavoie.opencabinet.drawing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.daniellavoie.opencabinet.shop.domain.model.Units;

/**
 * Same results as the app's {@code formatLength}.
 */
class LengthFormatTest {
	@Test
	void imperialToTheNearestSixteenth() {
		assertThat(LengthFormat.format(876.3, Units.IMPERIAL)).isEqualTo("34 1/2\"");
		assertThat(LengthFormat.format(457.2, Units.IMPERIAL)).isEqualTo("18\"");
		assertThat(LengthFormat.format(3.175, Units.IMPERIAL)).isEqualTo("1/8\"");
		assertThat(LengthFormat.format(15.9, Units.IMPERIAL)).isEqualTo("5/8\"");
		assertThat(LengthFormat.format(-12.7, Units.IMPERIAL)).isEqualTo("-1/2\"");
	}

	@Test
	void metricToTheMillimetre() {
		assertThat(LengthFormat.format(876.3, Units.METRIC)).isEqualTo("876 mm");
	}
}
