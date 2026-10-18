package dev.daniellavoie.opencabinet.assistant.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.Violation;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A candidate design for a Project (ADR 0004): the steps the assistant took from the design it
 * started on, each passing the Construction Rules, the design they lead to, and what it still needs
 * to know. It changes nothing until a Member accepts it, which replays the steps onto the Project's
 * current design.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class DesignProposal {
	UUID id;
	UUID shopId;
	UUID projectId;
	UUID conversationId;
	ProposalStatus status;
	Design baseDesign;
	Design design;
	List<DesignStep> steps;
	String summary;
	List<String> openQuestions;
	List<String> assumptions;
	UUID acceptedBy;
	LocalDateTime acceptedTimestamp;
	/**
	 * The Project's design before accepting, restored by Undo accept while nobody has saved since.
	 */
	Design designBeforeAccept;
	LocalDateTime acceptedProjectTimestamp;
	/**
	 * How many steps accepting applied; when fewer than all, the step it stopped at and why.
	 */
	Integer appliedSteps;
	Integer refusedStep;
	List<Violation> refusedViolations;
	LocalDateTime creationTimestamp;
	LocalDateTime updateTimestamp;
}
