package dev.daniellavoie.opencabinet.assistant.domain.model;

/**
 * Why the assistant stopped without a full answer.
 */
public enum AnswerError {
	/**
	 * The Shop turned the assistant off.
	 */
	OFF,
	/**
	 * This month's budget is spent.
	 */
	BUDGET_SPENT,
	/**
	 * The model could not be reached or failed.
	 */
	MODEL_ERROR,
	/**
	 * It took more steps than one request allows.
	 */
	TOO_MANY_STEPS,
	/**
	 * The server restarted while it was working.
	 */
	INTERRUPTED;
}
