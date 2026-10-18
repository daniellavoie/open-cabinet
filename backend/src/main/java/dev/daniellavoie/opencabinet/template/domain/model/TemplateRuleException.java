package dev.daniellavoie.opencabinet.template.domain.model;

import java.util.List;

import dev.daniellavoie.opencabinet.project.domain.TemplateRules;
import lombok.Getter;

/**
 * A template that breaks the template rules cannot be published.
 */
@Getter
public class TemplateRuleException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final List<TemplateRules.Violation> violations;

	public TemplateRuleException(List<TemplateRules.Violation> violations) {
		super("the template breaks " + violations.size() + " template rule(s)");
		this.violations = List.copyOf(violations);
	}
}
