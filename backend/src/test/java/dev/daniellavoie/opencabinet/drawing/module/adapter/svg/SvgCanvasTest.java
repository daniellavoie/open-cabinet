package dev.daniellavoie.opencabinet.drawing.module.adapter.svg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.daniellavoie.opencabinet.drawing.domain.model.Drawing;
import dev.daniellavoie.opencabinet.drawing.domain.model.Point;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape;
import dev.daniellavoie.opencabinet.drawing.domain.model.TextStyle;

class SvgCanvasTest {
	@Test
	void escapesText() {
		var svg = SvgCanvas.render(new Drawing("x", List.of(new Shape.Text(new Point(0, 0), "<C1 & \"x\">",
				TextStyle.LABEL, 0))));

		assertThat(svg).contains("&lt;C1 &amp; &quot;x&quot;&gt;").doesNotContain("<C1");
	}
}
