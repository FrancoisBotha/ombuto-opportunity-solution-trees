package com.opportunity.tree.service.dto.backup;

import com.opportunity.tree.domain.enumeration.AssumptionStatus;
import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import com.opportunity.tree.domain.enumeration.SolutionStatus;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Point-in-time snapshot of the whole application data set, produced by the admin backup
 * endpoint (BKRST-001). Relationships are expressed by id so the graph can be rebuilt on
 * restore without needing insert ordering hints.
 */
public record BackupArchive(
    String formatVersion,
    String applicationVersion,
    Instant exportedAt,
    Map<String, Integer> counts,
    List<TeamRow> teams,
    List<TeamMemberRow> teamMembers,
    List<ProductRow> products,
    List<OutcomeRow> outcomes,
    List<OpportunityRow> opportunities,
    List<SolutionRow> solutions,
    List<AssumptionRow> assumptions,
    List<EvidenceRow> evidences,
    List<InterviewRow> interviews,
    List<CommentRow> comments,
    List<NodeLinkRow> nodeLinks,
    List<OpenQuestionRow> openQuestions,
    List<TagRow> tags,
    List<NodeHistoryRow> nodeHistories,
    List<JoinRow> opportunityInterviews,
    List<JoinRow> opportunityTags,
    List<JoinRow> solutionTags
) {
    public static final String FORMAT_VERSION = "1";

    /**
     * Whether all fields that identify the versioned backup envelope are present. Row-level
     * database constraints are enforced later inside the restore transaction, but this check is
     * deliberately completed before that transaction can delete any data.
     */
    public boolean hasExpectedEnvelope() {
        return (
            formatVersion != null &&
            applicationVersion != null &&
            exportedAt != null &&
            counts != null &&
            present(teams) &&
            present(teamMembers) &&
            present(products) &&
            present(outcomes) &&
            present(opportunities) &&
            present(solutions) &&
            present(assumptions) &&
            present(evidences) &&
            present(interviews) &&
            present(comments) &&
            present(nodeLinks) &&
            present(openQuestions) &&
            present(tags) &&
            present(nodeHistories) &&
            present(opportunityInterviews) &&
            present(opportunityTags) &&
            present(solutionTags)
        );
    }

    private static boolean present(List<?> rows) {
        return rows != null && rows.stream().noneMatch(java.util.Objects::isNull);
    }

    public record TeamRow(Long id, String name, String description, Instant createdDate) {}

    public record TeamMemberRow(Long id, TeamRole role, Instant joinedDate, Long teamId, String userId) {}

    public record ProductRow(
        Long id,
        String name,
        String description,
        String vision,
        Boolean archived,
        Integer sortOrder,
        Instant createdDate,
        Long teamId
    ) {}

    public record OutcomeRow(
        Long id,
        String title,
        String description,
        Integer sortOrder,
        Instant createdDate,
        Instant lastModifiedDate,
        Long productId,
        String ownerId
    ) {}

    public record OpportunityRow(
        Long id,
        String title,
        String description,
        OpportunityStatus status,
        Integer valuerating,
        Integer priority,
        Integer sortOrder,
        Instant createdDate,
        Instant lastModifiedDate,
        Long outcomeId,
        Long parentId,
        String ownerId
    ) {}

    public record SolutionRow(
        Long id,
        String title,
        String description,
        SolutionStatus status,
        Integer sortOrder,
        Instant createdDate,
        Instant lastModifiedDate,
        Long opportunityId,
        String ownerId
    ) {}

    public record AssumptionRow(
        Long id,
        String statement,
        String description,
        AssumptionStatus status,
        Integer confidence,
        Integer sortOrder,
        Instant createdDate,
        Instant lastModifiedDate,
        Long solutionId,
        String ownerId
    ) {}

    public record EvidenceRow(
        Long id,
        String title,
        String description,
        Integer sortOrder,
        Instant createdDate,
        Instant lastModifiedDate,
        Long opportunityId,
        Long assumptionId
    ) {}

    public record InterviewRow(
        Long id,
        String title,
        String participant,
        LocalDate interviewDate,
        String notes,
        String recordingUrl,
        Instant createdDate,
        Long productId,
        String interviewerId
    ) {}

    public record CommentRow(
        Long id,
        String body,
        Instant createdDate,
        Instant editedDate,
        String authorId,
        Long outcomeId,
        Long opportunityId,
        Long solutionId,
        Long assumptionId,
        Long evidenceId
    ) {}

    public record NodeLinkRow(
        Long id,
        String name,
        String url,
        Integer sortOrder,
        Instant createdDate,
        Long productId,
        Long outcomeId,
        Long opportunityId,
        Long solutionId,
        Long assumptionId,
        Long evidenceId
    ) {}

    public record OpenQuestionRow(Long id, String questionText, Boolean done, Integer sortOrder, Instant createdDate, Long opportunityId) {}

    public record TagRow(Long id, String name, String colour, Long teamId) {}

    public record NodeHistoryRow(
        Long id,
        TreeNodeType nodeType,
        Long nodeId,
        HistoryEventType eventType,
        String summary,
        Instant createdDate,
        String authorId
    ) {}

    /** A row of a many-to-many join table (opportunity/interview, opportunity/tag, solution/tag). */
    public record JoinRow(Long leftId, Long rightId) {}
}
