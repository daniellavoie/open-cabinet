package dev.daniellavoie.opencabinet.assistant.scenario;

import java.util.ArrayList;
import java.util.List;

import dev.daniellavoie.opencabinet.assistant.domain.model.ModelReply;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelTier;
import dev.daniellavoie.opencabinet.assistant.domain.port.backend.AssistantModel;

/**
 * A model that records the replies of another, or replays recorded replies in order.
 */
final class RecordingModel implements AssistantModel {
	private final AssistantModel delegate;
	private final List<Recording.Reply> replies;
	private int next;

	private RecordingModel(AssistantModel delegate, List<Recording.Reply> replies) {
		this.delegate = delegate;
		this.replies = replies;
	}

	static RecordingModel recording(AssistantModel delegate) {
		return new RecordingModel(delegate, new ArrayList<>());
	}

	static RecordingModel replaying(Recording recording) {
		return new RecordingModel(null, recording.replies());
	}

	List<Recording.Reply> replies() {
		return List.copyOf(replies);
	}

	/**
	 * Whether a replay used every recorded reply.
	 */
	boolean exhausted() {
		return next == replies.size();
	}

	@Override
	public boolean isConfigured() {
		return true;
	}

	@Override
	public String modelFor(ModelTier tier) {
		return delegate == null ? "recorded" : delegate.modelFor(tier);
	}

	@Override
	public ModelReply complete(ModelRequest request) {
		if (delegate != null) {
			var reply = delegate.complete(request);
			replies.add(new Recording.Reply(reply.getText(), reply.getToolCalls(), reply.getInputTokens(), reply.getOutputTokens(),
					reply.getCostUsd()));
			return reply;
		}
		if (next >= replies.size()) {
			throw new IllegalStateException("the recording has no reply left: the domain asked the model more than when it was recorded");
		}
		var reply = replies.get(next++);
		return ModelReply.builder()
				.text(reply.text())
				.toolCalls(reply.toolCalls() == null ? List.of() : reply.toolCalls())
				.model("recorded")
				.inputTokens(reply.inputTokens())
				.outputTokens(reply.outputTokens())
				.costUsd(reply.costUsd())
				.build();
	}
}
