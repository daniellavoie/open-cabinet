package dev.daniellavoie.opencabinet.assistant.module.adapter.database.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import dev.daniellavoie.opencabinet.assistant.domain.model.DesignProposal;
import dev.daniellavoie.opencabinet.assistant.domain.model.DesignStep;
import dev.daniellavoie.opencabinet.assistant.domain.model.ProposalStatus;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.Remark;
import dev.daniellavoie.opencabinet.project.domain.model.Violation;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "DesignProposal")
@Table(name = "design_proposal")
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DesignProposalEntity {
	@Id
	UUID id;
	UUID shopId;
	UUID projectId;
	UUID conversationId;
	@Enumerated(EnumType.STRING)
	ProposalStatus status;
	@JdbcTypeCode(SqlTypes.JSON)
	Design baseDesign;
	@JdbcTypeCode(SqlTypes.JSON)
	Design design;
	@JdbcTypeCode(SqlTypes.JSON)
	List<DesignStep> steps;
	String summary;
	@JdbcTypeCode(SqlTypes.JSON)
	List<String> openQuestions;
	@JdbcTypeCode(SqlTypes.JSON)
	List<String> assumptions;
	@JdbcTypeCode(SqlTypes.JSON)
	List<Remark> remarks;
	@JdbcTypeCode(SqlTypes.JSON)
	Map<String, String> remarkExplanations;
	UUID acceptedBy;
	LocalDateTime acceptedTimestamp;
	@JdbcTypeCode(SqlTypes.JSON)
	Design designBeforeAccept;
	LocalDateTime acceptedProjectTimestamp;
	Integer appliedSteps;
	Integer refusedStep;
	@JdbcTypeCode(SqlTypes.JSON)
	List<Violation> refusedViolations;
	LocalDateTime creationTimestamp;
	LocalDateTime updateTimestamp;

	public static DesignProposalEntity from(DesignProposal proposal) {
		return DesignProposalEntity.builder()
				.id(proposal.getId())
				.shopId(proposal.getShopId())
				.projectId(proposal.getProjectId())
				.conversationId(proposal.getConversationId())
				.status(proposal.getStatus())
				.baseDesign(proposal.getBaseDesign())
				.design(proposal.getDesign())
				.steps(proposal.getSteps())
				.summary(proposal.getSummary())
				.openQuestions(proposal.getOpenQuestions())
				.assumptions(proposal.getAssumptions())
				.remarks(proposal.getRemarks())
				.remarkExplanations(proposal.getRemarkExplanations())
				.acceptedBy(proposal.getAcceptedBy())
				.acceptedTimestamp(proposal.getAcceptedTimestamp())
				.designBeforeAccept(proposal.getDesignBeforeAccept())
				.acceptedProjectTimestamp(proposal.getAcceptedProjectTimestamp())
				.appliedSteps(proposal.getAppliedSteps())
				.refusedStep(proposal.getRefusedStep())
				.refusedViolations(proposal.getRefusedViolations())
				.creationTimestamp(proposal.getCreationTimestamp())
				.updateTimestamp(proposal.getUpdateTimestamp())
				.build();
	}

	public DesignProposal to() {
		return DesignProposal.builder()
				.id(id)
				.shopId(shopId)
				.projectId(projectId)
				.conversationId(conversationId)
				.status(status)
				.baseDesign(baseDesign)
				.design(design)
				.steps(steps)
				.summary(summary)
				.openQuestions(openQuestions)
				.assumptions(assumptions)
				.remarks(remarks)
				.remarkExplanations(remarkExplanations)
				.acceptedBy(acceptedBy)
				.acceptedTimestamp(acceptedTimestamp)
				.designBeforeAccept(designBeforeAccept)
				.acceptedProjectTimestamp(acceptedProjectTimestamp)
				.appliedSteps(appliedSteps)
				.refusedStep(refusedStep)
				.refusedViolations(refusedViolations)
				.creationTimestamp(creationTimestamp)
				.updateTimestamp(updateTimestamp)
				.build();
	}
}
