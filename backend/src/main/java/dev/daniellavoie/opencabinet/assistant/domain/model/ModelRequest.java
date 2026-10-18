package dev.daniellavoie.opencabinet.assistant.domain.model;

import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * One request to the model: instructions, the conversation so far, oldest first, and the tools it
 * may call. The model never runs a tool: it asks for calls, and the domain runs them and answers.
 */
@Value
@Builder(toBuilder = true)
public class ModelRequest {
	ModelTier tier;
	/**
	 * What the request is for, recorded with its cost.
	 */
	String purpose;
	String instructions;
	List<ModelMessage> messages;
	@Builder.Default
	List<ToolSpec> tools = List.of();
	int maxTokens;

	/**
	 * A message from the Member (with the photos, sketches and PDFs attached to it), the model's
	 * answer (with the tool calls it asked for), or the result of a tool call.
	 */
	public record ModelMessage(Speaker speaker, String text, List<ModelAttachment> attachments, List<ToolCall> toolCalls,
			ToolResult toolResult) {
		public ModelMessage(Speaker speaker, String text) {
			this(speaker, text, List.of(), List.of(), null);
		}

		public static ModelMessage member(String text, List<ModelAttachment> attachments) {
			return new ModelMessage(Speaker.MEMBER, text, attachments, List.of(), null);
		}

		public static ModelMessage assistant(String text, List<ToolCall> toolCalls) {
			return new ModelMessage(Speaker.ASSISTANT, text, List.of(), toolCalls, null);
		}

		public static ModelMessage tool(ToolResult result) {
			return new ModelMessage(Speaker.TOOL, null, List.of(), List.of(), result);
		}
	}

	/**
	 * A photo, sketch or PDF sent with a Member's message.
	 */
	public record ModelAttachment(String mediaType, byte[] bytes) {
	}

	public enum Speaker {
		MEMBER, ASSISTANT, TOOL;
	}

	/**
	 * A tool the model may call: its arguments follow the JSON Schema.
	 */
	public record ToolSpec(String name, String description, String inputSchema) {
	}

	/**
	 * A call the model asks for, with its arguments as JSON.
	 */
	public record ToolCall(String id, String name, String arguments) {
	}

	public record ToolResult(String callId, String name, String content) {
	}
}
