package dev.daniellavoie.opencabinet.project.domain.model;

import java.util.List;

import lombok.Getter;

@Getter
public class ConstructionRuleException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final List<Violation> violations;

	public ConstructionRuleException(List<Violation> violations) {
		super("the design breaks " + violations.size() + " construction rule(s)");
		this.violations = List.copyOf(violations);
	}
}
