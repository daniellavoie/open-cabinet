package dev.daniellavoie.opencabinet;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.StreamSupport;

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
				.andExpect(jsonPath("$", hasSize(13)));

		var exported = mockMvc.perform(as(owner, get("/api/material/export")))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		var repriced = exported.replace("MEL-WH-58,68.00", "MEL-WH-58,72.00")
				+ "HARDWARE,Pentures de porte,HNG-SOFT,7.25,,,,false\r\n";
		mockMvc.perform(as(owner, post("/api/material/import"))
				.contentType("text/csv")
				.content(repriced))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.applied").value(true))
				.andExpect(jsonPath("$.created").value(1))
				.andExpect(jsonPath("$.updated").value(13));
		mockMvc.perform(as(owner, post("/api/material/import"))
				.contentType("text/csv")
				.content("kind,name,sku,price\nSHEET,Mélamine,MEL-WH-58,abc\nHARDWARE,Coulisse,MEL-WH-14,1\n"))
				.andExpect(jsonPath("$.applied").value(false))
				.andExpect(jsonPath("$.errors", hasSize(2)))
				.andExpect(jsonPath("$.errors[1].line").value(3));
		mockMvc.perform(as(owner, get("/api/material")))
				.andExpect(jsonPath("$", hasSize(14)));

		mockMvc.perform(as(owner, post("/api/shop-template"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": "Vanité 30", "cabinet": {"id": "c1", "templateCode": "BASE_DOOR", "wallId": "w1",
						"offsetMm": 300, "widthMm": 762, "heightMm": 876.3, "depthMm": 533.4, "elevationMm": 0,
						"shelfCount": 1, "dividerPositionsMm": [381], "finishedLeft": true}}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.baseCode").value("BASE_DOOR"));
		mockMvc.perform(as(owner, get("/api/shop-template")))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].preset.widthMm").value(762))
				.andExpect(jsonPath("$[0].preset.dividerPositionsMm[0]").value(381))
				.andExpect(jsonPath("$[0].preset.finishedLeft").value(true));

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

		mockMvc.perform(as(owner, put("/api/project/" + projectId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(update.toString()))
				.andExpect(status().isConflict());
		mockMvc.perform(as(owner, post("/api/project/" + projectId + "/lock")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mine").value(true));

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

		mockMvc.perform(as(owner, get("/api/quote/draft")))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].content.projectName").value("Cuisine"));

		mockMvc.perform(as(owner, post("/api/quote/" + quoteId + "/send")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SENT"));

		mockMvc.perform(as(owner, post("/api/quote/" + quoteId + "/send")))
				.andExpect(status().isConflict());

		var revision = read(mockMvc.perform(as(owner, post("/api/quote/" + quoteId + "/revise")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DRAFT"))
				.andExpect(jsonPath("$.displayNumber").value("S-0001-R1")));
		mockMvc.perform(as(owner, post("/api/quote/" + quoteId + "/revise")))
				.andExpect(status().isConflict());
		mockMvc.perform(as(owner, post("/api/quote/" + revision.get("id").asText() + "/send")))
				.andExpect(status().isOk());
		mockMvc.perform(as(owner, get("/api/quote/" + quoteId)))
				.andExpect(jsonPath("$.status").value("SUPERSEDED"));
		mockMvc.perform(as(owner, get("/api/project/" + projectId + "/quote")))
				.andExpect(jsonPath("$[0].displayNumber").value("S-0001-R1"))
				.andExpect(jsonPath("$[1].displayNumber").value("S-0001"));

		mockMvc.perform(as(owner, get("/api/quote/" + quoteId + "/pdf")))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", startsWith("application/pdf")));

		mockMvc.perform(as("stranger-" + System.nanoTime(), get("/api/project/" + projectId)))
				.andExpect(status().isForbidden());
	}

	@Test
	void ownersInviteMembersWhoJoinWithTheirRole() throws Exception {
		var owner = "owner-" + System.nanoTime();
		var designer = "designer-" + System.nanoTime();

		mockMvc.perform(as(owner, post("/api/shop"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"shopName\":\"Atelier Villeray\",\"memberName\":\"Luc\"}"))
				.andExpect(status().isOk());

		var created = read(mockMvc.perform(as(owner, post("/api/invitation"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"sophie@example.ca\",\"role\":\"DESIGNER\"}"))
				.andExpect(status().isOk()));
		var token = created.get("token").asText();

		mockMvc.perform(as(owner, get("/api/invitation")))
				.andExpect(jsonPath("$", hasSize(1)));

		mockMvc.perform(as(designer, get("/api/invitation/token/" + token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.shopName").value("Atelier Villeray"))
				.andExpect(jsonPath("$.role").value("DESIGNER"))
				.andExpect(jsonPath("$.invitedByName").value("Luc"));

		mockMvc.perform(as(designer, post("/api/invitation/token/" + token + "/accept"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"memberName\":\"Sophie\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("DESIGNER"))
				.andExpect(jsonPath("$.email").value("sophie@example.ca"));

		mockMvc.perform(as("someone-" + System.nanoTime(), post("/api/invitation/token/" + token + "/accept"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"memberName\":\"Again\"}"))
				.andExpect(status().isNotFound());

		mockMvc.perform(as(owner, get("/api/invitation")))
				.andExpect(jsonPath("$", hasSize(0)));
		mockMvc.perform(as(owner, get("/api/member")))
				.andExpect(jsonPath("$", hasSize(2)));

		mockMvc.perform(as(designer, get("/api/shop")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Atelier Villeray"));
		mockMvc.perform(as(designer, get("/api/member")))
				.andExpect(status().isForbidden());
		mockMvc.perform(as(designer, post("/api/invitation"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"role\":\"OWNER\"}"))
				.andExpect(status().isForbidden());

		var members = read(mockMvc.perform(as(owner, get("/api/member"))));
		var ownerId = StreamSupport.stream(members.spliterator(), false)
				.filter(member -> member.get("role").asText().equals("OWNER"))
				.map(member -> member.get("id").asText())
				.findFirst()
				.orElseThrow();

		var client = read(mockMvc.perform(as(owner, post("/api/client"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Nadia\"}")));
		var project = read(mockMvc.perform(as(owner, post("/api/project"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"clientId\":\"" + client.get("id").asText() + "\",\"name\":\"Vanité\"}")));
		var projectId = project.get("id").asText();
		var update = objectMapper.createObjectNode()
				.put("clientId", client.get("id").asText())
				.put("name", "Vanité")
				.set("design", project.get("design"));

		mockMvc.perform(as(owner, post("/api/project/" + projectId + "/lock")))
				.andExpect(jsonPath("$.mine").value(true));
		mockMvc.perform(as(designer, post("/api/project/" + projectId + "/lock")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mine").value(false))
				.andExpect(jsonPath("$.holderName").value("Luc"));
		mockMvc.perform(as(designer, put("/api/project/" + projectId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(update.toString()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("this project is being edited by Luc"));
		mockMvc.perform(as(owner, delete("/api/project/" + projectId + "/lock")))
				.andExpect(status().isNoContent());
		mockMvc.perform(as(designer, post("/api/project/" + projectId + "/lock")))
				.andExpect(jsonPath("$.mine").value(true));
		mockMvc.perform(as(designer, put("/api/project/" + projectId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(update.toString()))
				.andExpect(status().isOk());

		mockMvc.perform(as(owner, put("/api/member/" + ownerId + "/role"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"role\":\"DESIGNER\"}"))
				.andExpect(status().isConflict());
		mockMvc.perform(as(owner, delete("/api/member/" + ownerId)))
				.andExpect(status().isConflict());
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
