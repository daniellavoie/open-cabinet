package dev.daniellavoie.opencabinet.template.module.job;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import dev.daniellavoie.opencabinet.template.domain.PresetConversion;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Converts any Milestone 3 presets left when the application starts.
 */
@Component
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PresetConversionJob implements ApplicationRunner {
	PresetConversion conversion;

	@Override
	public void run(ApplicationArguments arguments) {
		conversion.convertAll();
	}
}
