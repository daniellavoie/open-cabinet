package dev.daniellavoie.opencabinet;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Boots the whole application against a real PostgreSQL, running the Flyway migrations, and walks
 * through the first milestone: create a Shop, a Client and a Project, design it, read its Cut List
 * and quote it. Skipped when Docker is not available.
 */
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
		"open-cabinet.keycloak.issuer-uri=https://keycloak.test/realms/open-cabinet",
		"open-cabinet.keycloak.client-id=open-cabinet-web" })
class OpenCabinetApplicationTests {
	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Test
	void healthIsUp() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
	}

	@Test
	void designCutListAndQuoteAProject() throws Exception {
		var owner = "owner-" + System.nanoTime();

		mockMvc.perform(as(owner, get("/api/member/me")))
				.andExpect(status().isNotFound());

		mockMvc.perform(as(owner, post("/api/shop"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"shopName\":\"Atelier Rosemont\",\"memberName\":\"Marie\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currency").value("CAD"))
				.andExpect(jsonPath("$.settings.taxes", hasSize(2)));

		mockMvc.perform(as(owner, get("/api/member/me")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("OWNER"));

		mockMvc.perform(as(owner, get("/api/material")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(11)));

		var client = read(mockMvc.perform(as(owner, post("/api/client"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Jean Tremblay\",\"address\":\"123 rue Masson, Montréal\"}"))
				.andExpect(status().isOk()));

		var project = read(mockMvc.perform(as(owner, post("/api/project"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"clientId\":\"" + client.get("id").asText() + "\",\"name\":\"Cuisine\"}"))
				.andExpect(status().isOk()));
		var projectId = project.get("id").asText();
		var wallId = project.at("/design/walls/0/id").asText();

		var update = objectMapper.createObjectNode();
		update.put("clientId", client.get("id").asText());
		update.put("name", "Cuisine");
		var design = (ObjectNode) project.get("design").deepCopy();
		var cabinets = design.putArray("cabinets");
		cabinets.add(cabinet("c1", "BASE_DOOR", wallId, 0, 457.2, 876.3, 609.6, 0, 1));
		cabinets.add(cabinet("c2", "BASE_DRAWERS", wallId, 457.2, 457.2, 876.3, 609.6, 0, 0));
		cabinets.add(cabinet("c3", "WALL_DOOR", wallId, 0, 914.4, 762, 304.8, 1371.6, 2));
		update.set("design", design);
		update.putArray("manualLines").addObject().put("description", "Installation").put("amount", 400);

		var overlapping = update.deepCopy();
		((ObjectNode) overlapping.at("/design/cabinets/1")).put("offsetMm", 100);
		mockMvc.perform(as(owner, put("/api/project/" + projectId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(overlapping.toString()))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.details[0].code").value("OVERLAP"));

		mockMvc.perform(as(owner, put("/api/project/" + projectId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(update.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.design.cabinets", hasSize(3)));

		mockMvc.perform(as(owner, get("/api/project/" + projectId + "/output")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cabinetLabels.c1").value("C1"))
				.andExpect(jsonPath("$.operations.cabinets").value(3))
				.andExpect(jsonPath("$.nesting[0].sheets[0].placements").isNotEmpty());

		var quote = read(mockMvc.perform(as(owner, post("/api/project/" + projectId + "/quote")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DRAFT"))
				.andExpect(jsonPath("$.displayNumber").value("S-0001"))
				.andExpect(jsonPath("$.content.lines", hasSize(2))));
		var quoteId = quote.get("id").asText();

		mockMvc.perform(as(owner, post("/api/quote/" + quoteId + "/send")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SENT"));

		mockMvc.perform(as(owner, post("/api/quote/" + quoteId + "/send")))
				.andExpect(status().isConflict());

		mockMvc.perform(as(owner, get("/api/quote/" + quoteId + "/pdf")))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", startsWith("application/pdf")));

		mockMvc.perform(as("stranger-" + System.nanoTime(), get("/api/project/" + projectId)))
				.andExpect(status().isForbidden());
	}

	private ObjectNode cabinet(String id, String template, String wallId, double offset, double width, double height,
			double depth, double elevation, int shelves) {
		return objectMapper.createObjectNode()
				.put("id", id)
				.put("templateCode", template)
				.put("wallId", wallId)
				.put("offsetMm", offset)
				.put("widthMm", width)
				.put("heightMm", height)
				.put("depthMm", depth)
				.put("elevationMm", elevation)
				.put("shelfCount", shelves);
	}

	private static MockHttpServletRequestBuilder as(String subject, MockHttpServletRequestBuilder request) {
		return request.with(jwt().jwt(token -> token.subject(subject)));
	}

	private JsonNode read(org.springframework.test.web.servlet.ResultActions result) throws Exception {
		return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
	}
}
