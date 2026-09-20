package com.opportunity.tree.service;

import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Comment;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.Interview;
import com.opportunity.tree.domain.NodeHistory;
import com.opportunity.tree.domain.NodeLink;
import com.opportunity.tree.domain.OpenQuestion;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Tag;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
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
import com.opportunity.tree.service.dto.backup.BackupRestoreSummary;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOG = LoggerFactory.getLogger(BackupService.class);

    /** The single sequence every generated entity id comes from (see the initial Liquibase changelog). */
    static final String ID_SEQUENCE = "sequence_generator";

    /** {@code allocationSize} of the entities' {@code @SequenceGenerator}: the restart leaves this much head room. */
    static final int ID_SEQUENCE_ALLOCATION_SIZE = 50;

    /** At most this many unresolvable references are named in the 400 body; the rest are counted. */
    public static final int MAX_REPORTED_REFERENCES = 20;

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

    /**
     * Every reference in the archive that cannot be resolved in this installation, described in
     * words (BKRST fix C5). Two kinds are checked, both <em>before</em> the restore transaction so
     * nothing has been deleted when the answer comes back:
     *
     * <ul>
     *   <li>users: {@code jhi_user} is deliberately not part of a backup, so every login the
     *       archive re-attaches rows to (members, owners, authors, interviewers) has to already
     *       exist here — restoring a production archive onto a fresh install otherwise fails deep
     *       inside the insert phase, with everything already deleted;</li>
     *   <li>rows inside the archive itself: a truncated or hand-edited file can point a product at
     *       a team, or an opportunity at an outcome, that it does not carry.</li>
     * </ul>
     *
     * @return the unresolvable references, empty when the archive can be restored as it stands
     */
    @Transactional(readOnly = true)
    public List<String> unresolvableReferences(BackupArchive archive) {
        List<String> problems = new ArrayList<>();
        problems.addAll(missingUsers(archive));
        problems.addAll(danglingRowReferences(archive));
        return problems;
    }

    private List<String> missingUsers(BackupArchive archive) {
        Set<String> referenced = new LinkedHashSet<>();
        collect(referenced, archive.teamMembers().stream().map(BackupArchive.TeamMemberRow::userId));
        collect(referenced, archive.outcomes().stream().map(BackupArchive.OutcomeRow::ownerId));
        collect(referenced, archive.opportunities().stream().map(BackupArchive.OpportunityRow::ownerId));
        collect(referenced, archive.solutions().stream().map(BackupArchive.SolutionRow::ownerId));
        collect(referenced, archive.assumptions().stream().map(BackupArchive.AssumptionRow::ownerId));
        collect(referenced, archive.interviews().stream().map(BackupArchive.InterviewRow::interviewerId));
        collect(referenced, archive.comments().stream().map(BackupArchive.CommentRow::authorId));
        collect(referenced, archive.nodeHistories().stream().map(BackupArchive.NodeHistoryRow::authorId));
        if (referenced.isEmpty()) {
            return List.of();
        }
        Set<String> known = new HashSet<>(
            em.createQuery("select u.id from User u where u.id in :ids", String.class).setParameter("ids", referenced).getResultList()
        );
        return referenced.stream().filter(id -> !known.contains(id)).map(id -> "user " + id).toList();
    }

    private List<String> danglingRowReferences(BackupArchive archive) {
        Set<Long> teams = ids(archive.teams().stream().map(BackupArchive.TeamRow::id));
        Set<Long> products = ids(archive.products().stream().map(BackupArchive.ProductRow::id));
        Set<Long> outcomes = ids(archive.outcomes().stream().map(BackupArchive.OutcomeRow::id));
        Set<Long> opportunities = ids(archive.opportunities().stream().map(BackupArchive.OpportunityRow::id));
        Set<Long> solutions = ids(archive.solutions().stream().map(BackupArchive.SolutionRow::id));
        Set<Long> assumptions = ids(archive.assumptions().stream().map(BackupArchive.AssumptionRow::id));
        Set<Long> evidences = ids(archive.evidences().stream().map(BackupArchive.EvidenceRow::id));
        Set<Long> interviews = ids(archive.interviews().stream().map(BackupArchive.InterviewRow::id));
        Set<Long> tags = ids(archive.tags().stream().map(BackupArchive.TagRow::id));

        List<String> problems = new ArrayList<>();
        archive.teamMembers().forEach(r -> check(problems, "team member " + r.id(), "team", r.teamId(), teams));
        archive.products().forEach(r -> check(problems, "product " + r.id(), "team", r.teamId(), teams));
        archive.tags().forEach(r -> check(problems, "tag " + r.id(), "team", r.teamId(), teams));
        archive.outcomes().forEach(r -> check(problems, "outcome " + r.id(), "product", r.productId(), products));
        archive.interviews().forEach(r -> check(problems, "interview " + r.id(), "product", r.productId(), products));
        archive.opportunities().forEach(r -> {
            check(problems, "opportunity " + r.id(), "outcome", r.outcomeId(), outcomes);
            check(problems, "opportunity " + r.id(), "parent opportunity", r.parentId(), opportunities);
        });
        archive.solutions().forEach(r -> check(problems, "solution " + r.id(), "opportunity", r.opportunityId(), opportunities));
        archive.assumptions().forEach(r -> check(problems, "assumption " + r.id(), "solution", r.solutionId(), solutions));
        archive.evidences().forEach(r -> {
            check(problems, "evidence " + r.id(), "opportunity", r.opportunityId(), opportunities);
            check(problems, "evidence " + r.id(), "assumption", r.assumptionId(), assumptions);
        });
        archive.openQuestions().forEach(r -> check(problems, "open question " + r.id(), "opportunity", r.opportunityId(), opportunities));
        archive.comments().forEach(r -> {
            String owner = "comment " + r.id();
            check(problems, owner, "outcome", r.outcomeId(), outcomes);
            check(problems, owner, "opportunity", r.opportunityId(), opportunities);
            check(problems, owner, "solution", r.solutionId(), solutions);
            check(problems, owner, "assumption", r.assumptionId(), assumptions);
            check(problems, owner, "evidence", r.evidenceId(), evidences);
        });
        archive.nodeLinks().forEach(r -> {
            String owner = "link " + r.id();
            check(problems, owner, "product", r.productId(), products);
            check(problems, owner, "outcome", r.outcomeId(), outcomes);
            check(problems, owner, "opportunity", r.opportunityId(), opportunities);
            check(problems, owner, "solution", r.solutionId(), solutions);
            check(problems, owner, "assumption", r.assumptionId(), assumptions);
            check(problems, owner, "evidence", r.evidenceId(), evidences);
        });
        archive.opportunityInterviews().forEach(r -> {
            check(problems, "opportunity/interview link", "opportunity", r.leftId(), opportunities);
            check(problems, "opportunity/interview link", "interview", r.rightId(), interviews);
        });
        archive.opportunityTags().forEach(r -> {
            check(problems, "opportunity/tag link", "opportunity", r.leftId(), opportunities);
            check(problems, "opportunity/tag link", "tag", r.rightId(), tags);
        });
        archive.solutionTags().forEach(r -> {
            check(problems, "solution/tag link", "solution", r.leftId(), solutions);
            check(problems, "solution/tag link", "tag", r.rightId(), tags);
        });
        return problems;
    }

    private static void check(List<String> problems, String owner, String targetType, Long targetId, Set<Long> known) {
        if (targetId != null && !known.contains(targetId)) {
            problems.add(owner + " refers to missing " + targetType + " " + targetId);
        }
    }

    private static void collect(Collection<String> into, Stream<String> ids) {
        ids.filter(java.util.Objects::nonNull).forEach(into::add);
    }

    private static Set<Long> ids(Stream<Long> ids) {
        return ids.filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Replaces every application-data row with the rows in a previously validated archive.
     * Deletion, insertion and join-table rebuilding are one transaction, so any constraint or
     * persistence failure rolls the entire replacement back.
     *
     * <p>Callers must have checked {@link #unresolvableReferences(BackupArchive)} first and must
     * hold the {@link RestoreMutex}.
     */
    @Transactional
    public BackupRestoreSummary restoreAll(BackupArchive archive) {
        lockEveryTeam();
        deleteAllApplicationData();

        Map<Long, Team> teams = new HashMap<>();
        for (BackupArchive.TeamRow row : archive.teams()) {
            Team restored = replicate(
                new Team().id(row.id()).name(row.name()).description(row.description()).createdDate(row.createdDate())
            );
            teams.put(row.id(), restored);
        }
        em.flush();

        for (BackupArchive.TeamMemberRow row : archive.teamMembers()) {
            replicate(
                new TeamMember()
                    .id(row.id())
                    .role(row.role())
                    .joinedDate(row.joinedDate())
                    .team(teams.get(row.teamId()))
                    .user(userReference(row.userId()))
            );
        }
        em.flush();

        Map<Long, Product> products = new HashMap<>();
        for (BackupArchive.ProductRow row : archive.products()) {
            Product restored = replicate(
                new Product()
                    .id(row.id())
                    .name(row.name())
                    .description(row.description())
                    .vision(row.vision())
                    .archived(row.archived())
                    .sortOrder(row.sortOrder())
                    .createdDate(row.createdDate())
                    .team(teams.get(row.teamId()))
            );
            products.put(row.id(), restored);
        }
        em.flush();

        Map<Long, Outcome> outcomes = new HashMap<>();
        for (BackupArchive.OutcomeRow row : archive.outcomes()) {
            Outcome restored = replicate(
                new Outcome()
                    .id(row.id())
                    .title(row.title())
                    .description(row.description())
                    .sortOrder(row.sortOrder())
                    .createdDate(row.createdDate())
                    .lastModifiedDate(row.lastModifiedDate())
                    .product(products.get(row.productId()))
                    .owner(userReference(row.ownerId()))
            );
            outcomes.put(row.id(), restored);
        }
        em.flush();

        Map<Long, Opportunity> opportunities = new HashMap<>();
        for (BackupArchive.OpportunityRow row : archive.opportunities()) {
            Opportunity restored = replicate(
                new Opportunity()
                    .id(row.id())
                    .title(row.title())
                    .description(row.description())
                    .status(row.status())
                    .valuerating(row.valuerating())
                    .priority(row.priority())
                    .sortOrder(row.sortOrder())
                    .createdDate(row.createdDate())
                    .lastModifiedDate(row.lastModifiedDate())
                    .outcome(outcomes.get(row.outcomeId()))
                    .owner(userReference(row.ownerId()))
            );
            opportunities.put(row.id(), restored);
        }
        // Insert every opportunity with its outcome first, then wire the self-referencing tree.
        em.flush();
        for (BackupArchive.OpportunityRow row : archive.opportunities()) {
            if (row.parentId() != null) {
                opportunities.get(row.id()).setParent(opportunities.get(row.parentId()));
            }
        }
        em.flush();

        Map<Long, Solution> solutions = new HashMap<>();
        for (BackupArchive.SolutionRow row : archive.solutions()) {
            Solution restored = replicate(
                new Solution()
                    .id(row.id())
                    .title(row.title())
                    .description(row.description())
                    .status(row.status())
                    .sortOrder(row.sortOrder())
                    .createdDate(row.createdDate())
                    .lastModifiedDate(row.lastModifiedDate())
                    .opportunity(opportunities.get(row.opportunityId()))
                    .owner(userReference(row.ownerId()))
            );
            solutions.put(row.id(), restored);
        }
        em.flush();

        Map<Long, Assumption> assumptions = new HashMap<>();
        for (BackupArchive.AssumptionRow row : archive.assumptions()) {
            Assumption restored = replicate(
                new Assumption()
                    .id(row.id())
                    .statement(row.statement())
                    .description(row.description())
                    .status(row.status())
                    .confidence(row.confidence())
                    .sortOrder(row.sortOrder())
                    .createdDate(row.createdDate())
                    .lastModifiedDate(row.lastModifiedDate())
                    .solution(solutions.get(row.solutionId()))
                    .owner(userReference(row.ownerId()))
            );
            assumptions.put(row.id(), restored);
        }
        em.flush();

        Map<Long, Evidence> evidences = new HashMap<>();
        for (BackupArchive.EvidenceRow row : archive.evidences()) {
            Evidence restored = replicate(
                new Evidence()
                    .id(row.id())
                    .title(row.title())
                    .description(row.description())
                    .sortOrder(row.sortOrder())
                    .createdDate(row.createdDate())
                    .lastModifiedDate(row.lastModifiedDate())
                    .opportunity(opportunities.get(row.opportunityId()))
                    .assumption(assumptions.get(row.assumptionId()))
            );
            evidences.put(row.id(), restored);
        }
        em.flush();

        Map<Long, Interview> interviews = new HashMap<>();
        for (BackupArchive.InterviewRow row : archive.interviews()) {
            Interview restored = replicate(
                new Interview()
                    .id(row.id())
                    .title(row.title())
                    .participant(row.participant())
                    .interviewDate(row.interviewDate())
                    .notes(row.notes())
                    .recordingUrl(row.recordingUrl())
                    .createdDate(row.createdDate())
                    .product(products.get(row.productId()))
                    .interviewer(userReference(row.interviewerId()))
            );
            interviews.put(row.id(), restored);
        }
        em.flush();

        Map<Long, Tag> tags = new HashMap<>();
        for (BackupArchive.TagRow row : archive.tags()) {
            Tag restored = replicate(new Tag().id(row.id()).name(row.name()).colour(row.colour()).team(teams.get(row.teamId())));
            tags.put(row.id(), restored);
        }
        em.flush();

        for (BackupArchive.CommentRow row : archive.comments()) {
            replicate(
                new Comment()
                    .id(row.id())
                    .body(row.body())
                    .createdDate(row.createdDate())
                    .editedDate(row.editedDate())
                    .author(userReference(row.authorId()))
                    .outcome(outcomes.get(row.outcomeId()))
                    .opportunity(opportunities.get(row.opportunityId()))
                    .solution(solutions.get(row.solutionId()))
                    .assumption(assumptions.get(row.assumptionId()))
                    .evidence(evidences.get(row.evidenceId()))
            );
        }
        em.flush();

        for (BackupArchive.NodeLinkRow row : archive.nodeLinks()) {
            replicate(
                new NodeLink()
                    .id(row.id())
                    .name(row.name())
                    .url(row.url())
                    .sortOrder(row.sortOrder())
                    .createdDate(row.createdDate())
                    .product(products.get(row.productId()))
                    .outcome(outcomes.get(row.outcomeId()))
                    .opportunity(opportunities.get(row.opportunityId()))
                    .solution(solutions.get(row.solutionId()))
                    .assumption(assumptions.get(row.assumptionId()))
                    .evidence(evidences.get(row.evidenceId()))
            );
        }
        em.flush();

        for (BackupArchive.OpenQuestionRow row : archive.openQuestions()) {
            replicate(
                new OpenQuestion()
                    .id(row.id())
                    .questionText(row.questionText())
                    .done(row.done())
                    .sortOrder(row.sortOrder())
                    .createdDate(row.createdDate())
                    .opportunity(opportunities.get(row.opportunityId()))
            );
        }
        em.flush();

        for (BackupArchive.NodeHistoryRow row : archive.nodeHistories()) {
            replicate(
                new NodeHistory()
                    .id(row.id())
                    .nodeType(row.nodeType())
                    .nodeId(row.nodeId())
                    .eventType(row.eventType())
                    .summary(row.summary())
                    .createdDate(row.createdDate())
                    .author(userReference(row.authorId()))
            );
        }
        em.flush();

        insertJoinRows("rel_opportunity__interview", "opportunity_id", "interview_id", archive.opportunityInterviews());
        insertJoinRows("rel_opportunity__tag", "opportunity_id", "tag_id", archive.opportunityTags());
        insertJoinRows("rel_solution__tag", "solution_id", "tag_id", archive.solutionTags());

        restartIdSequence(archive);

        Map<String, Integer> counts = restoredCounts(archive);
        return new BackupRestoreSummary(archive.exportedAt(), counts);
    }

    /**
     * Takes the tree-structure lock of every existing team, lowest id first, before anything is
     * deleted (BKRST fix C6). A restore is the one write that touches all teams at once, so it
     * queues behind — and then blocks — the per-team writes that {@link TreeStructureLock} guards,
     * instead of deleting rows from under a create or a move that is half done. Lowest id first is
     * the same order {@link TreeStructureLock#lockTeams} uses, so the two cannot deadlock.
     */
    private void lockEveryTeam() {
        em
            .createQuery("select t from Team t order by t.id", Team.class)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .setHint("jakarta.persistence.lock.timeout", TreeStructureLock.LOCK_TIMEOUT_MS)
            .getResultList();
    }

    /**
     * Moves {@code sequence_generator} past the highest id the archive brought in (BKRST fix C2).
     *
     * <p>Restored rows keep their original ids — they are written with {@code Session.replicate},
     * which never calls the generator — so on a fresh database the sequence is still sitting at its
     * initial value and the very first node created after a restore would be handed an id the
     * archive has already used. The restart leaves one {@code allocationSize} of head room above
     * the highest restored id, and never moves the sequence <em>backwards</em>: the current value is
     * read first (consuming one id, which is free) and kept if it is already higher, so ids handed
     * out to rows this backup does not cover stay unique too.
     */
    private void restartIdSequence(BackupArchive archive) {
        long highestRestoredId = highestRestoredId(archive);
        long current = nextSequenceValue();
        long restartAt = Math.max(current, highestRestoredId + ID_SEQUENCE_ALLOCATION_SIZE);
        LOG.info("Restarting {} at {} after restoring ids up to {}", ID_SEQUENCE, restartAt, highestRestoredId);
        // No bind parameter: ALTER SEQUENCE takes a literal. restartAt is a long we computed.
        em.createNativeQuery("alter sequence " + ID_SEQUENCE + " restart with " + restartAt).executeUpdate();
    }

    /** Reads (and consumes) the sequence's next value, portably across H2 and PostgreSQL. */
    private long nextSequenceValue() {
        String sql = em
            .unwrap(SessionImplementor.class)
            .getFactory()
            .getJdbcServices()
            .getDialect()
            .getSequenceSupport()
            .getSequenceNextValString(ID_SEQUENCE);
        // H2 phrases it as "call next value for ...", which JDBC cannot run as a query.
        if (sql.regionMatches(true, 0, "call ", 0, 5)) {
            sql = "select " + sql.substring(5);
        }
        return toLong(em.createNativeQuery(sql).getSingleResult());
    }

    private static long highestRestoredId(BackupArchive archive) {
        return Stream.of(
            archive.teams().stream().map(BackupArchive.TeamRow::id),
            archive.teamMembers().stream().map(BackupArchive.TeamMemberRow::id),
            archive.products().stream().map(BackupArchive.ProductRow::id),
            archive.outcomes().stream().map(BackupArchive.OutcomeRow::id),
            archive.opportunities().stream().map(BackupArchive.OpportunityRow::id),
            archive.solutions().stream().map(BackupArchive.SolutionRow::id),
            archive.assumptions().stream().map(BackupArchive.AssumptionRow::id),
            archive.evidences().stream().map(BackupArchive.EvidenceRow::id),
            archive.interviews().stream().map(BackupArchive.InterviewRow::id),
            archive.comments().stream().map(BackupArchive.CommentRow::id),
            archive.nodeLinks().stream().map(BackupArchive.NodeLinkRow::id),
            archive.openQuestions().stream().map(BackupArchive.OpenQuestionRow::id),
            archive.tags().stream().map(BackupArchive.TagRow::id),
            archive.nodeHistories().stream().map(BackupArchive.NodeHistoryRow::id)
        )
            .flatMap(Function.identity())
            .filter(java.util.Objects::nonNull)
            .mapToLong(Long::longValue)
            .max()
            .orElse(0L);
    }

    private void deleteAllApplicationData() {
        em.flush();
        em.createNativeQuery("delete from rel_opportunity__interview").executeUpdate();
        em.createNativeQuery("delete from rel_opportunity__tag").executeUpdate();
        em.createNativeQuery("delete from rel_solution__tag").executeUpdate();
        em.createNativeQuery("delete from node_history").executeUpdate();
        em.createNativeQuery("delete from open_question").executeUpdate();
        em.createNativeQuery("delete from node_link").executeUpdate();
        em.createNativeQuery("delete from comment").executeUpdate();
        em.createNativeQuery("delete from tag").executeUpdate();
        em.createNativeQuery("delete from interview").executeUpdate();
        em.createNativeQuery("delete from evidence").executeUpdate();
        em.createNativeQuery("delete from assumption").executeUpdate();
        em.createNativeQuery("delete from solution").executeUpdate();
        em.createNativeQuery("update opportunity set parent_id = null").executeUpdate();
        em.createNativeQuery("delete from opportunity").executeUpdate();
        em.createNativeQuery("delete from outcome").executeUpdate();
        em.createNativeQuery("delete from product").executeUpdate();
        em.createNativeQuery("delete from team_member").executeUpdate();
        em.createNativeQuery("delete from team").executeUpdate();
        em.clear();
    }

    private User userReference(String id) {
        return id == null ? null : em.getReference(User.class, id);
    }

    private <E> E replicate(E entity) {
        em.unwrap(Session.class).replicate(entity, ReplicationMode.EXCEPTION);
        return entity;
    }

    private void insertJoinRows(String table, String leftColumn, String rightColumn, List<BackupArchive.JoinRow> rows) {
        for (BackupArchive.JoinRow row : rows) {
            em
                .createNativeQuery("insert into " + table + " (" + leftColumn + ", " + rightColumn + ") values (:leftId, :rightId)")
                .setParameter("leftId", row.leftId())
                .setParameter("rightId", row.rightId())
                .executeUpdate();
        }
    }

    private Map<String, Integer> restoredCounts(BackupArchive archive) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("teams", archive.teams().size());
        counts.put("teamMembers", archive.teamMembers().size());
        counts.put("products", archive.products().size());
        counts.put("outcomes", archive.outcomes().size());
        counts.put("opportunities", archive.opportunities().size());
        counts.put("solutions", archive.solutions().size());
        counts.put("assumptions", archive.assumptions().size());
        counts.put("evidences", archive.evidences().size());
        counts.put("interviews", archive.interviews().size());
        counts.put("tags", archive.tags().size());
        counts.put("comments", archive.comments().size());
        counts.put("nodeLinks", archive.nodeLinks().size());
        counts.put("openQuestions", archive.openQuestions().size());
        counts.put("nodeHistories", archive.nodeHistories().size());
        counts.put("opportunityInterviews", archive.opportunityInterviews().size());
        counts.put("opportunityTags", archive.opportunityTags().size());
        counts.put("solutionTags", archive.solutionTags().size());
        counts.put(
            "treeNodes",
            archive.outcomes().size() +
                archive.opportunities().size() +
                archive.solutions().size() +
                archive.assumptions().size() +
                archive.evidences().size()
        );
        return counts;
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
