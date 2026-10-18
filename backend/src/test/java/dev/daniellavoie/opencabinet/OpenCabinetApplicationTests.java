package dev.daniellavoie.opencabinet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.template.domain.PresetConversion;
import dev.daniellavoie.opencabinet.template.domain.model.ShopCabinetTemplate;
import dev.daniellavoie.opencabinet.template.domain.port.backend.ShopCabinetTemplateRepository;

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
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	ShopCabinetTemplateRepository presets;

	@Autowired
	PresetConversion presetConversion;

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

		// The Design Assistant is off until an Owner opts in, and cannot be turned on without a key.
		mockMvc.perform(as(owner, get("/api/assistant")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.configured").value(false))
				.andExpect(jsonPath("$.enabled").value(false))
				.andExpect(jsonPath("$.spentThisMonthUsd").value(0))
				.andExpect(jsonPath("$.builderModel").value("claude-sonnet-5-5"));
		mockMvc.perform(as(owner, put("/api/assistant"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"enabled\": true, \"monthlyBudgetUsd\": 20}"))
				.andExpect(status().isConflict());
		mockMvc.perform(as(owner, put("/api/assistant"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"enabled\": false, \"monthlyBudgetUsd\": 20}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.monthlyBudgetUsd").value(20));
		mockMvc.perform(as(owner, post("/api/assistant/check")))
				.andExpect(status().isConflict());

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

		// A Milestone 3 preset becomes a Shop library template, its size kept as the default.
		var shopId = UUID.fromString(read(mockMvc.perform(as(owner, get("/api/shop")))).get("id").asString());
		presets.save(ShopCabinetTemplate.builder().id(UUID.randomUUID()).name("Vanité 30").baseCode("BASE_DOOR")
				.preset(Cabinet.builder().templateCode("BASE_DOOR").templateVersion(1).widthMm(762).heightMm(876.3)
						.depthMm(533.4).shelfCount(1).build())
				.build(), shopId);
		assertThat(presetConversion.convertAll()).isEqualTo(1);
		assertThat(presetConversion.convertAll()).isZero();
		mockMvc.perform(as(owner, get("/api/template")))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].name").value("Vanité 30"))
				.andExpect(jsonPath("$[0].shared").value(true))
				.andExpect(jsonPath("$[0].versions[0].defaultWidthMm").value(762))
				.andExpect(jsonPath("$[0].versions[0].assembly.parts").isNotEmpty());

		var client = read(mockMvc.perform(as(owner, post("/api/client"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Jean Tremblay\",\"address\":\"123 rue Masson, Montréal\"}"))
				.andExpect(status().isOk()));

		var project = read(mockMvc.perform(as(owner, post("/api/project"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"clientId\":\"" + client.get("id").asString() + "\",\"name\":\"Cuisine\"}"))
				.andExpect(status().isOk()));
		var projectId = project.get("id").asString();
		var wallId = project.at("/design/walls/0/id").asString();

		var update = objectMapper.createObjectNode();
		update.put("clientId", client.get("id").asString());
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
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].code").value("OVERLAP"));

		update.put("nestingLayout", "CROSSCUT_FIRST");
		mockMvc.perform(as(owner, put("/api/project/" + projectId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(update.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.design.cabinets", hasSize(3)))
				.andExpect(jsonPath("$.nestingLayout").value("CROSSCUT_FIRST"));

		// A design saved before a field existed still loads, the field at its default.
		jdbcTemplate.update("UPDATE project SET design = jsonb_set(design, '{cabinets,0}', (design #> '{cabinets,0}') - 'mirrored' - 'shelfCount') WHERE id = ?::uuid",
				projectId);
		mockMvc.perform(as(owner, get("/api/project/" + projectId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.design.cabinets[0].mirrored").value(false))
				.andExpect(jsonPath("$.design.cabinets[0].shelfCount").value(0));
		mockMvc.perform(as(owner, put("/api/project/" + projectId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(update.toString()))
				.andExpect(status().isOk());

		mockMvc.perform(as(owner, get("/api/project/" + projectId + "/output")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cabinetLabels.c1").value("C1"))
				.andExpect(jsonPath("$.nesting[0].layout").value("CROSSCUT_FIRST"))
				.andExpect(jsonPath("$.nesting[0].alternatives", hasSize(2)))
				.andExpect(jsonPath("$.nesting[0].sheets[0].cuts").isNotEmpty())
				.andExpect(jsonPath("$.operations.cabinets").value(3))
				.andExpect(jsonPath("$.nesting[0].sheets[0].placements").isNotEmpty())
				.andExpect(jsonPath("$.parts").doesNotExist());

		mockMvc.perform(as(owner, get("/api/project/" + projectId + "/shop-drawings/pdf?language=en")))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", startsWith("application/pdf")));

		for (var document : List.of("cut-list", "nesting", "bill-of-materials")) {
			mockMvc.perform(as(owner, get("/api/project/" + projectId + "/output/" + document + "/pdf")))
					.andExpect(status().isOk())
					.andExpect(header().string("Content-Type", startsWith("application/pdf")));
		}
		var cutListCsv = mockMvc.perform(as(owner, get("/api/project/" + projectId + "/output/cut-list/csv")))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", startsWith("text/csv")))
				.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		assertThat(cutListCsv.lines().findFirst()).hasValueSatisfying(line -> assertThat(line).contains("part,material"));
		mockMvc.perform(as(owner, get("/api/project/" + projectId + "/output/unknown/csv")))
				.andExpect(status().isBadRequest());

		// A template made from Cabinet c1 starts from its starter template drawn as parts, at its size.
		var template = read(mockMvc.perform(as(owner, post("/api/project/" + projectId + "/project-template"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"Vanité\", \"cabinetId\": \"c1\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.draft.assembly.parts").isNotEmpty())
				.andExpect(jsonPath("$.sourceCabinetId").value("c1"))
				.andExpect(jsonPath("$.versions", hasSize(0))));
		var templateId = template.get("id").asString();
		var draft = (ObjectNode) template.get("draft");
		mockMvc.perform(as(owner, put("/api/project-template/" + templateId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.createObjectNode().put("name", "Vanité 18").set("draft", draft).toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Vanité 18"));
		mockMvc.perform(as(owner, post("/api/project-template/" + templateId + "/publish"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.versions", hasSize(1)))
				.andExpect(jsonPath("$.versions[0].code").value(templateId))
				.andExpect(jsonPath("$.versions[0].name").value("Vanité 18"));
		mockMvc.perform(as(owner, post("/api/project-template/" + templateId + "/share")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.shared").value(true));
		mockMvc.perform(as(owner, post("/api/project-template/" + templateId + "/share")))
				.andExpect(status().isConflict());
		mockMvc.perform(as(owner, get("/api/template")))
				.andExpect(jsonPath("$", hasSize(2)));
		mockMvc.perform(as(owner, put("/api/project-template/" + templateId + "/archived"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("true"))
				.andExpect(jsonPath("$.archived").value(true));
		mockMvc.perform(as(owner, get("/api/project/" + projectId + "/cabinet-template")))
				.andExpect(jsonPath("$[?(@.code == '" + templateId + "')].archived").value(true));
		mockMvc.perform(as(owner, put("/api/project-template/" + templateId + "/archived"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("false"))
				.andExpect(jsonPath("$.archived").value(false));
		mockMvc.perform(as(owner, delete("/api/project-template/" + templateId)))
				.andExpect(status().isConflict());
		var libraryDraft = read(mockMvc.perform(as(owner, post("/api/template"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"Colonne maison\", \"templateCode\": \"TALL_PANTRY\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.shared").value(true))
				.andExpect(jsonPath("$.projectId").doesNotExist()));
		mockMvc.perform(as(owner, delete("/api/project-template/" + libraryDraft.get("id").asString())))
				.andExpect(status().isNoContent());
		var fromScratch = read(mockMvc.perform(as(owner, post("/api/template"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"Caisson mural maison\", \"type\": \"WALL\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.draft.type").value("WALL"))
				.andExpect(jsonPath("$.draft.defaultElevationMm").value(greaterThan(0.0)))
				.andExpect(jsonPath("$.draft.assembly.parts").isEmpty())
				.andExpect(jsonPath("$.draft.assembly.hardware").isEmpty()));
		mockMvc.perform(as(owner, delete("/api/project-template/" + fromScratch.get("id").asString())))
				.andExpect(status().isNoContent());
		mockMvc.perform(as(owner, post("/api/project-template/" + templateId + "/publish"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest());
		var tooWide = draft.deepCopy().put("maxWidthMm", 2600);
		mockMvc.perform(as(owner, put("/api/project-template/" + templateId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.createObjectNode().put("name", "Vanité 18").set("draft", tooWide).toString()))
				.andExpect(status().isOk());
		mockMvc.perform(as(owner, post("/api/project-template/" + templateId + "/publish"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"changeNote\": \"Plus large\"}"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].code").value("TOO_BIG_FOR_SHEET"));
		mockMvc.perform(as(owner, get("/api/project/" + projectId + "/cabinet-template")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.code == '" + templateId + "')].version").value(1));

		var withTemplate = update.deepCopy();
		((ObjectNode) withTemplate.get("design").get("cabinets").get(0)).put("templateCode", templateId)
				.put("templateVersion", 1);
		mockMvc.perform(as(owner, put("/api/project/" + projectId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(withTemplate.toString()))
				.andExpect(status().isOk());
		mockMvc.perform(as(owner, get("/api/project/" + projectId + "/output")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.operations.cabinets").value(3));

		var quote = read(mockMvc.perform(as(owner, post("/api/project/" + projectId + "/quote")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DRAFT"))
				.andExpect(jsonPath("$.pictures").doesNotExist())
				.andExpect(jsonPath("$.displayNumber").value("S-0001"))
				.andExpect(jsonPath("$.content.lines", hasSize(2))));
		var quoteId = quote.get("id").asString();

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
		mockMvc.perform(as(owner, post("/api/quote/" + revision.get("id").asString() + "/send")))
				.andExpect(status().isOk());
		mockMvc.perform(as(owner, get("/api/quote/" + quoteId)))
				.andExpect(jsonPath("$.status").value("SUPERSEDED"));
		mockMvc.perform(as(owner, get("/api/project/" + projectId + "/quote")))
				.andExpect(jsonPath("$[0].displayNumber").value("S-0001-R1"))
				.andExpect(jsonPath("$[1].displayNumber").value("S-0001"));

		var quotePdf = mockMvc.perform(as(owner, get("/api/quote/" + quoteId + "/pdf")))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", startsWith("application/pdf")))
				.andReturn().getResponse().getContentAsByteArray();
		// The pictures were stored with the Quote and printed after the price.
		var quoteReader = new PdfReader(quotePdf);
		assertThat(new PdfTextExtractor(quoteReader).getTextFromPage(quoteReader.getNumberOfPages()))
				.contains("Élévation — Mur 1");

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
		var token = created.get("token").asString();

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
		mockMvc.perform(as(designer, get("/api/assistant")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.enabled").value(false));
		mockMvc.perform(as(designer, put("/api/assistant"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"enabled\": false, \"monthlyBudgetUsd\": 20}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(as(designer, post("/api/invitation"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"role\":\"OWNER\"}"))
				.andExpect(status().isForbidden());

		var members = read(mockMvc.perform(as(owner, get("/api/member"))));
		var ownerId = StreamSupport.stream(members.spliterator(), false)
				.filter(member -> member.get("role").asString().equals("OWNER"))
				.map(member -> member.get("id").asString())
				.findFirst()
				.orElseThrow();

		var client = read(mockMvc.perform(as(owner, post("/api/client"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Nadia\"}")));
		var project = read(mockMvc.perform(as(owner, post("/api/project"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"clientId\":\"" + client.get("id").asString() + "\",\"name\":\"Vanité\"}")));
		var projectId = project.get("id").asString();
		var update = objectMapper.createObjectNode()
				.put("clientId", client.get("id").asString())
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
				.put("templateVersion", 1)
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
