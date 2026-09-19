package com.opportunity.tree.service;

import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Comment;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.repository.AssumptionRepository;
import com.opportunity.tree.repository.CommentRepository;
import com.opportunity.tree.repository.EvidenceRepository;
import com.opportunity.tree.repository.InterviewRepository;
import com.opportunity.tree.repository.NodeHistoryRepository;
import com.opportunity.tree.repository.NodeLinkRepository;
import com.opportunity.tree.repository.OpenQuestionRepository;
import com.opportunity.tree.repository.OpportunityRepository;
import com.opportunity.tree.repository.OutcomeRepository;
import com.opportunity.tree.repository.ProductRepository;
import com.opportunity.tree.repository.SolutionRepository;
import com.opportunity.tree.repository.TagRepository;
import com.opportunity.tree.repository.TeamMemberRepository;
import com.opportunity.tree.repository.TeamRepository;
import com.opportunity.tree.service.dto.backup.BackupArchive;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Produces the versioned backup archive returned by the admin backup endpoint (BKRST-001).
 *
 * <p>The whole read runs in a single read-only transaction so the resulting archive is
 * internally consistent even if editors keep changing the tree while the export is running.
 */
@Service
public class BackupService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProductRepository productRepository;
    private final OutcomeRepository outcomeRepository;
    private final OpportunityRepository opportunityRepository;
    private final SolutionRepository solutionRepository;
    private final AssumptionRepository assumptionRepository;
    private final EvidenceRepository evidenceRepository;
    private final InterviewRepository interviewRepository;
    private final CommentRepository commentRepository;
    private final NodeLinkRepository nodeLinkRepository;
    private final OpenQuestionRepository openQuestionRepository;
    private final TagRepository tagRepository;
    private final NodeHistoryRepository nodeHistoryRepository;
    private final EntityManager em;
    private final String applicationVersion;

    public BackupService(
        TeamRepository teamRepository,
        TeamMemberRepository teamMemberRepository,
        ProductRepository productRepository,
        OutcomeRepository outcomeRepository,
        OpportunityRepository opportunityRepository,
        SolutionRepository solutionRepository,
        AssumptionRepository assumptionRepository,
        EvidenceRepository evidenceRepository,
        InterviewRepository interviewRepository,
        CommentRepository commentRepository,
        NodeLinkRepository nodeLinkRepository,
        OpenQuestionRepository openQuestionRepository,
        TagRepository tagRepository,
        NodeHistoryRepository nodeHistoryRepository,
        EntityManager em,
        @Value("${jhipster.api-docs.version:0.0.0}") String applicationVersion
    ) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.productRepository = productRepository;
        this.outcomeRepository = outcomeRepository;
        this.opportunityRepository = opportunityRepository;
        this.solutionRepository = solutionRepository;
        this.assumptionRepository = assumptionRepository;
        this.evidenceRepository = evidenceRepository;
        this.interviewRepository = interviewRepository;
        this.commentRepository = commentRepository;
        this.nodeLinkRepository = nodeLinkRepository;
        this.openQuestionRepository = openQuestionRepository;
        this.tagRepository = tagRepository;
        this.nodeHistoryRepository = nodeHistoryRepository;
        this.em = em;
        this.applicationVersion = applicationVersion;
    }

    @Transactional(readOnly = true)
    public BackupArchive exportAll(java.time.Instant exportedAt) {
        List<BackupArchive.TeamRow> teams = teamRepository
            .findAll()
            .stream()
            .map(t -> new BackupArchive.TeamRow(t.getId(), t.getName(), t.getDescription(), t.getCreatedDate()))
            .toList();

        List<BackupArchive.TeamMemberRow> teamMembers = teamMemberRepository
            .findAll()
            .stream()
            .map(m ->
                new BackupArchive.TeamMemberRow(
                    m.getId(),
                    m.getRole(),
                    m.getJoinedDate(),
                    idOf(m.getTeam(), Team::getId),
                    m.getUser() == null ? null : m.getUser().getId()
                )
            )
            .toList();

        List<BackupArchive.ProductRow> products = productRepository
            .findAll()
            .stream()
            .map(p ->
                new BackupArchive.ProductRow(
                    p.getId(),
                    p.getName(),
                    p.getDescription(),
                    p.getVision(),
                    p.getArchived(),
                    p.getSortOrder(),
                    p.getCreatedDate(),
                    idOf(p.getTeam(), Team::getId)
                )
            )
            .toList();

        List<BackupArchive.OutcomeRow> outcomes = outcomeRepository
            .findAll()
            .stream()
            .map(o ->
                new BackupArchive.OutcomeRow(
                    o.getId(),
                    o.getTitle(),
                    o.getDescription(),
                    o.getSortOrder(),
                    o.getCreatedDate(),
                    o.getLastModifiedDate(),
                    idOf(o.getProduct(), Product::getId),
                    o.getOwner() == null ? null : o.getOwner().getId()
                )
            )
            .toList();

        List<BackupArchive.OpportunityRow> opportunities = opportunityRepository
            .findAll()
            .stream()
            .map(o ->
                new BackupArchive.OpportunityRow(
                    o.getId(),
                    o.getTitle(),
                    o.getDescription(),
                    o.getStatus(),
                    o.getValuerating(),
                    o.getPriority(),
                    o.getSortOrder(),
                    o.getCreatedDate(),
                    o.getLastModifiedDate(),
                    idOf(o.getOutcome(), Outcome::getId),
                    idOf(o.getParent(), Opportunity::getId),
                    o.getOwner() == null ? null : o.getOwner().getId()
                )
            )
            .toList();

        List<BackupArchive.SolutionRow> solutions = solutionRepository
            .findAll()
            .stream()
            .map(s ->
                new BackupArchive.SolutionRow(
                    s.getId(),
                    s.getTitle(),
                    s.getDescription(),
                    s.getStatus(),
                    s.getSortOrder(),
                    s.getCreatedDate(),
                    s.getLastModifiedDate(),
                    idOf(s.getOpportunity(), Opportunity::getId),
                    s.getOwner() == null ? null : s.getOwner().getId()
                )
            )
            .toList();

        List<BackupArchive.AssumptionRow> assumptions = assumptionRepository
            .findAll()
            .stream()
            .map(a ->
                new BackupArchive.AssumptionRow(
                    a.getId(),
                    a.getStatement(),
                    a.getDescription(),
                    a.getStatus(),
                    a.getConfidence(),
                    a.getSortOrder(),
                    a.getCreatedDate(),
                    a.getLastModifiedDate(),
                    idOf(a.getSolution(), Solution::getId),
                    a.getOwner() == null ? null : a.getOwner().getId()
                )
            )
            .toList();

        List<BackupArchive.EvidenceRow> evidences = evidenceRepository
            .findAll()
            .stream()
            .map(e ->
                new BackupArchive.EvidenceRow(
                    e.getId(),
                    e.getTitle(),
                    e.getDescription(),
                    e.getSortOrder(),
                    e.getCreatedDate(),
                    e.getLastModifiedDate(),
                    idOf(e.getOpportunity(), Opportunity::getId),
                    idOf(e.getAssumption(), Assumption::getId)
                )
            )
            .toList();

        List<BackupArchive.InterviewRow> interviews = interviewRepository
            .findAll()
            .stream()
            .map(i ->
                new BackupArchive.InterviewRow(
                    i.getId(),
                    i.getTitle(),
                    i.getParticipant(),
                    i.getInterviewDate(),
                    i.getNotes(),
                    i.getRecordingUrl(),
                    i.getCreatedDate(),
                    idOf(i.getProduct(), Product::getId),
                    i.getInterviewer() == null ? null : i.getInterviewer().getId()
                )
            )
            .toList();

        List<BackupArchive.CommentRow> comments = commentRepository
            .findAll()
            .stream()
            .map(c ->
                new BackupArchive.CommentRow(
                    c.getId(),
                    c.getBody(),
                    c.getCreatedDate(),
                    c.getEditedDate(),
                    c.getAuthor() == null ? null : c.getAuthor().getId(),
                    idOf(c.getParent(), Comment::getId),
                    idOf(c.getOutcome(), Outcome::getId),
                    idOf(c.getOpportunity(), Opportunity::getId),
                    idOf(c.getSolution(), Solution::getId),
                    idOf(c.getAssumption(), Assumption::getId),
                    idOf(c.getEvidence(), Evidence::getId)
                )
            )
            .toList();

        List<BackupArchive.NodeLinkRow> nodeLinks = nodeLinkRepository
            .findAll()
            .stream()
            .map(n ->
                new BackupArchive.NodeLinkRow(
                    n.getId(),
                    n.getName(),
                    n.getUrl(),
                    n.getSortOrder(),
                    n.getCreatedDate(),
                    idOf(n.getProduct(), Product::getId),
                    idOf(n.getOutcome(), Outcome::getId),
                    idOf(n.getOpportunity(), Opportunity::getId),
                    idOf(n.getSolution(), Solution::getId),
                    idOf(n.getAssumption(), Assumption::getId),
                    idOf(n.getEvidence(), Evidence::getId)
                )
            )
            .toList();

        List<BackupArchive.OpenQuestionRow> openQuestions = openQuestionRepository
            .findAll()
            .stream()
            .map(q ->
                new BackupArchive.OpenQuestionRow(
                    q.getId(),
                    q.getQuestionText(),
                    q.getDone(),
                    q.getSortOrder(),
                    q.getCreatedDate(),
                    idOf(q.getOpportunity(), Opportunity::getId)
                )
            )
            .toList();

        List<BackupArchive.TagRow> tags = tagRepository
            .findAll()
            .stream()
            .map(t -> new BackupArchive.TagRow(t.getId(), t.getName(), t.getColour(), idOf(t.getTeam(), Team::getId)))
            .toList();

        List<BackupArchive.NodeHistoryRow> nodeHistories = nodeHistoryRepository
            .findAll()
            .stream()
            .map(h ->
                new BackupArchive.NodeHistoryRow(
                    h.getId(),
                    h.getNodeType(),
                    h.getNodeId(),
                    h.getEventType(),
                    h.getSummary(),
                    h.getCreatedDate(),
                    h.getAuthor() == null ? null : h.getAuthor().getId()
                )
            )
            .toList();

        List<BackupArchive.JoinRow> opportunityInterviews = joinRows("select opportunity_id, interview_id from rel_opportunity__interview");
        List<BackupArchive.JoinRow> opportunityTags = joinRows("select opportunity_id, tag_id from rel_opportunity__tag");
        List<BackupArchive.JoinRow> solutionTags = joinRows("select solution_id, tag_id from rel_solution__tag");

        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("teams", teams.size());
        counts.put("teamMembers", teamMembers.size());
        counts.put("products", products.size());
        counts.put("outcomes", outcomes.size());
        counts.put("opportunities", opportunities.size());
        counts.put("solutions", solutions.size());
        counts.put("assumptions", assumptions.size());
        counts.put("evidences", evidences.size());
        counts.put("interviews", interviews.size());
        counts.put("comments", comments.size());
        counts.put("nodeLinks", nodeLinks.size());
        counts.put("openQuestions", openQuestions.size());
        counts.put("tags", tags.size());
        counts.put("nodeHistories", nodeHistories.size());
        counts.put("opportunityInterviews", opportunityInterviews.size());
        counts.put("opportunityTags", opportunityTags.size());
        counts.put("solutionTags", solutionTags.size());

        return new BackupArchive(
            BackupArchive.FORMAT_VERSION,
            applicationVersion,
            exportedAt,
            counts,
            teams,
            teamMembers,
            products,
            outcomes,
            opportunities,
            solutions,
            assumptions,
            evidences,
            interviews,
            comments,
            nodeLinks,
            openQuestions,
            tags,
            nodeHistories,
            opportunityInterviews,
            opportunityTags,
            solutionTags
        );
    }

    private static <E> Long idOf(E entity, Function<E, Long> getId) {
        return entity == null ? null : getId.apply(entity);
    }

    @SuppressWarnings("unchecked")
    private List<BackupArchive.JoinRow> joinRows(String sql) {
        List<Object[]> raw = em.createNativeQuery(sql).getResultList();
        return raw
            .stream()
            .map(r -> new BackupArchive.JoinRow(toLong(r[0]), toLong(r[1])))
            .toList();
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(value.toString());
    }
}
