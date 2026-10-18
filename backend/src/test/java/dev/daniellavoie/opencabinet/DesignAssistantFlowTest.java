package dev.daniellavoie.opencabinet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import dev.daniellavoie.opencabinet.assistant.domain.model.ModelReply;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest.ToolCall;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelTier;
import dev.daniellavoie.opencabinet.assistant.domain.port.backend.AssistantModel;

/**
 * The Design Assistant end to end, with a scripted model behind its port: a Member asks, the
 * request runs in the background, the Proposal is stored, accepted onto the Project, undone and
 * discarded. Skipped when Docker is not available.
 */
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
		"open-cabinet.keycloak.issuer-uri=https://keycloak.test/realms/open-cabinet",
		"open-cabinet.keycloak.client-id=open-cabinet-web" })
class DesignAssistantFlowTest {
	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockitoBean
	AssistantModel model;

	@Test
	void aMemberAsksAcceptsUndoesAndDiscards() throws Exception {
		var owner = "assistant-owner-" + System.nanoTime();
		Deque<ModelReply> replies = new ArrayDeque<>();
		when(model.isConfigured()).thenReturn(true);
		when(model.modelFor(any(ModelTier.class))).thenReturn("fake");
		when(model.complete(any(ModelRequest.class))).thenAnswer(invocation -> replies.removeFirst());

		mockMvc.perform(as(owner, post("/api/shop"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"shopName\":\"Atelier Limoilou\",\"memberName\":\"Marie\"}"))
				.andExpect(status().isOk());
		var client = read(mockMvc.perform(as(owner, post("/api/client"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Jean Tremblay\"}")));
		var project = read(mockMvc.perform(as(owner, post("/api/project"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"clientId\":\"" + client.get("id").asString() + "\",\"name\":\"Cuisine\"}")));
		var projectId = project.get("id").asString();
		var wallId = project.at("/design/walls/0/id").asString();
		var conversation = "/api/project/" + projectId + "/conversation";

		// Off until an Owner opts in.
		mockMvc.perform(as(owner, post(conversation + "/message"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"text\":\"Bonjour\"}"))
				.andExpect(status().isConflict());
		mockMvc.perform(as(owner, put("/api/assistant"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"enabled\": true, \"monthlyBudgetUsd\": 20}"))
				.andExpect(status().isOk());

		replies.add(reply(null, new ToolCall("1", "add_cabinet",
				"{\"templateCode\": \"BASE_DOOR\", \"wallId\": \"" + wallId + "\"}")));
		replies.add(reply(null, new ToolCall("2", "note_proposal",
				"{\"summary\": \"Un caisson bas\", \"openQuestions\": [], \"assumptions\": [\"Largeur de 18 po\"]}")));
		replies.add(reply("Voici un caisson bas."));
		mockMvc.perform(as(owner, post(conversation + "/message"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"text\":\"Ajoute un caisson bas\",\"language\":\"fr\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.messages[0].text").value("Ajoute un caisson bas"));

		var view = waitForAnswer(owner, conversation);
		assertThat(view.at("/messages/1/text").asString()).isEqualTo("Voici un caisson bas.");
		assertThat(view.at("/proposal/status").asString()).isEqualTo("OPEN");
		assertThat(view.at("/proposal/steps/0/tool").asString()).isEqualTo("add_cabinet");
		assertThat(view.at("/proposal/steps/0/arguments/templateCode").asString()).isEqualTo("BASE_DOOR");
		assertThat(view.at("/proposal/assumptions/0").asString()).isEqualTo("Largeur de 18 po");
		assertThat(view.at("/proposal/design/cabinets")).hasSize(1);
		var proposalId = view.at("/proposal/id").asString();
		var cabinetId = view.at("/proposal/steps/0/createdId").asString();

		// Usage is counted against the budget, once per round.
		mockMvc.perform(as(owner, get("/api/assistant")))
				.andExpect(jsonPath("$.spentThisMonthUsd").value(0.03));

		mockMvc.perform(as(owner, post("/api/project/" + projectId + "/proposal/" + proposalId + "/accept")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.appliedSteps").value(1))
				.andExpect(jsonPath("$.project.design.cabinets[0].id").value(cabinetId))
				.andExpect(jsonPath("$.proposal.status").value("ACCEPTED"));
		mockMvc.perform(as(owner, get("/api/project/" + projectId)))
				.andExpect(jsonPath("$.design.cabinets", org.hamcrest.Matchers.hasSize(1)));

		mockMvc.perform(as(owner, post("/api/project/" + projectId + "/proposal/" + proposalId + "/undo")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.proposal.status").value("OPEN"));
		mockMvc.perform(as(owner, get("/api/project/" + projectId)))
				.andExpect(jsonPath("$.design.cabinets", org.hamcrest.Matchers.hasSize(0)));

		mockMvc.perform(as(owner, post("/api/project/" + projectId + "/proposal/" + proposalId + "/discard")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DISCARDED"));
		mockMvc.perform(as(owner, get(conversation)))
				.andExpect(jsonPath("$.proposal").doesNotExist())
				.andExpect(jsonPath("$.messages", org.hamcrest.Matchers.hasSize(2)));
	}

	private JsonNode waitForAnswer(String subject, String conversation) throws Exception {
		for (var attempt = 0; attempt < 100; attempt++) {
			var view = read(mockMvc.perform(as(subject, get(conversation))));
			if (!view.get("running").asBoolean()) {
				return view;
			}
			Thread.sleep(50);
		}
		throw new AssertionError("the assistant did not answer");
	}

	private static ModelReply reply(String text, ToolCall... calls) {
		return ModelReply.builder().text(text).toolCalls(List.of(calls)).model("fake").inputTokens(100).outputTokens(10)
				.costUsd(new BigDecimal("0.01")).build();
	}

	private static MockHttpServletRequestBuilder as(String subject, MockHttpServletRequestBuilder request) {
		return request.with(jwt().jwt(token -> token.subject(subject)));
	}

	private JsonNode read(ResultActions result) throws Exception {
		return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
	}
}
