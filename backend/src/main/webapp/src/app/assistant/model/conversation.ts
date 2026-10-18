import { Design, Violation } from "../../project/model/design";
import { Project } from "../../project/model/project";

export type Author = "MEMBER" | "ASSISTANT";

/** Why the assistant stopped without a full answer. */
export type AnswerError = "OFF" | "BUDGET_SPENT" | "MODEL_ERROR" | "TOO_MANY_STEPS" | "INTERRUPTED";

/** A photo, sketch or PDF added to a message. */
export class Attachment {
  id!: string;
  fileName!: string;
  mediaType!: string;
  sizeBytes!: number;
}

export class ConversationMessage {
  id!: string;
  author!: Author;
  memberId!: string | null;
  memberName!: string | null;
  text!: string | null;
  error!: AnswerError | null;
  proposalId!: string | null;
  attachments!: Attachment[];
  creationTimestamp!: string;
}

export type ProgressKind = "THINKING" | "STEP" | "REFUSED" | "NOTED";

/** What the assistant is doing while it answers. */
export class ProgressEvent {
  sequence!: number;
  kind!: ProgressKind;
  tool!: string | null;
  arguments!: Record<string, unknown> | null;
  createdId!: string | null;
  violations!: Violation[] | null;
  problem!: string | null;
}

export class DesignStep {
  tool!: string;
  arguments!: Record<string, unknown>;
  createdId!: string | null;
}

export type ProposalStatus = "OPEN" | "ACCEPTED" | "DISCARDED";

/**
 * A candidate design built by the assistant (ADR 0004): it changes nothing
 * until a Member accepts it, which replays its steps onto the current design.
 */
export class DesignProposal {
  id!: string;
  projectId!: string;
  status!: ProposalStatus;
  design!: Design;
  steps!: DesignStep[];
  summary!: string | null;
  openQuestions!: string[];
  assumptions!: string[];
  acceptedTimestamp!: string | null;
  appliedSteps!: number | null;
  refusedStep!: number | null;
  refusedViolations!: Violation[] | null;
}

export class ConversationView {
  conversationId!: string | null;
  running!: boolean;
  messages!: ConversationMessage[];
  progress!: ProgressEvent[];
  /** The open Proposal, else the one accepted last. */
  proposal!: DesignProposal | null;
}

export class AcceptResult {
  project!: Project;
  proposal!: DesignProposal;
  appliedSteps!: number;
  totalSteps!: number;
  refusedStep!: number | null;
  violations!: Violation[] | null;
}
