package dev.daniellavoie.opencabinet.quote.domain.model;

import java.util.List;

import dev.daniellavoie.opencabinet.drawing.domain.model.View;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * The pictures printed with a Quote (the Room in 3D, Elevations), captured with its content so a
 * sent Quote keeps showing the design it priced.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class QuotePictures {
	List<View> views;
}
