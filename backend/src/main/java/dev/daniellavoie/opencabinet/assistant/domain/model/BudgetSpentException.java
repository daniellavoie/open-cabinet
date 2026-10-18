package dev.daniellavoie.opencabinet.assistant.domain.model;

/**
 * This month's Design Assistant budget is spent; nothing more is sent until next month.
 */
public class BudgetSpentException extends IllegalStateException {
	private static final long serialVersionUID = 1L;

	public BudgetSpentException() {
		super("this month's design assistant budget is spent");
	}
}
