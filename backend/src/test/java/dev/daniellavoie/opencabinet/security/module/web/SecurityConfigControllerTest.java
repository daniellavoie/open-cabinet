package dev.daniellavoie.opencabinet.security.module.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.daniellavoie.opencabinet.security.config.KeycloakConfiguration;
import dev.daniellavoie.opencabinet.security.config.SecurityConfig;

@WebMvcTest(controllers = SecurityConfigController.class, properties = {
		"open-cabinet.keycloak.issuer-uri=https://keycloak.test/realms/open-cabinet",
		"open-cabinet.keycloak.client-id=open-cabinet-web" })
@Import({ SecurityConfig.class, KeycloakConfiguration.class })
class SecurityConfigControllerTest {
	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	JwtDecoder jwtDecoder;

	@Test
	void configIsPublic() throws Exception {
		mockMvc.perform(get("/api/security/config"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.issuerUri").value("https://keycloak.test/realms/open-cabinet"))
				.andExpect(jsonPath("$.clientId").value("open-cabinet-web"));
	}

	@Test
	void otherApisRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/projects"))
				.andExpect(status().isUnauthorized());
	}
}
