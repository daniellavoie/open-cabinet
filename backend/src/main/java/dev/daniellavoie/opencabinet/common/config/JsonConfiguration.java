package dev.daniellavoie.opencabinet.common.config;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.type.format.jackson.Jackson3JsonFormatMapper;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * One JSON configuration for the API and for the designs, templates and settings stored as JSON.
 * A field absent from the JSON reads as its default (false, 0): designs saved before a field
 * existed still load, and the app may leave such fields out of requests.
 */
@Configuration
public class JsonConfiguration {
	public static <B extends JsonMapper.Builder> B configure(B builder) {
		builder.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
		return builder;
	}

	@Bean
	JsonMapperBuilderCustomizer lenientPrimitives() {
		return JsonConfiguration::configure;
	}

	/**
	 * Hibernate would otherwise build its own mapper, and pick Jackson 2 when the model provider's
	 * SDK brings it along.
	 */
	@Bean
	HibernatePropertiesCustomizer jsonFormatMapper(JsonMapper jsonMapper) {
		return properties -> properties.put(MappingSettings.JSON_FORMAT_MAPPER, new Jackson3JsonFormatMapper(jsonMapper));
	}
}
