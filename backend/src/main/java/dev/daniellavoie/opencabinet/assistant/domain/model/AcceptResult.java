package dev.daniellavoie.opencabinet.assistant.domain.model;

import java.util.List;

import dev.daniellavoie.opencabinet.project.domain.model.Project;
import dev.daniellavoie.opencabinet.project.domain.model.Violation;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * The saved Project after accepting a Proposal, and how many of its steps applied: replaying stops
 * at the first step that breaks a Construction Rule on the current design.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class AcceptResult {
	Project project;
	DesignProposal proposal;
	int appliedSteps;
	int totalSteps;
	Integer refusedStep;
	List<Violation> violations;
}
