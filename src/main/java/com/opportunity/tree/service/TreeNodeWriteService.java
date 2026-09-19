package com.opportunity.tree.service;

import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.AssumptionStatus;
import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import com.opportunity.tree.domain.enumeration.SolutionStatus;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.TeamMemberRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.service.dto.tree.CreateTreeNodeRequest;
import com.opportunity.tree.service.dto.tree.TreeNodeDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Create, patch and delete of tree nodes for the OST tree builder
 * ({@code POST /api/tree/nodes}, {@code PATCH|DELETE /api/tree/nodes/{type}/{id}}).
 *
 * <p>Re-homes the TREE-002 write rules that used to live in the generated
 * {@code *ServiceImpl} classes: the parent must exist, be in a team the caller can edit
 * (OWNER/EDITOR) and be a legal parent type ({@link TreeNodeRules}); server-set fields
 * ({@code sortOrder}, {@code createdDate}, {@code lastModifiedDate}) are never taken from the
 * client. Viewers, non-members and ROLE_ADMIN users who are not members get
 * {@link TeamAccessDeniedException} (403) whether or not the node exists.
 *
 * <p>History ({@link NodeHistoryRecorder}) is written in the same transaction: CREATED on
 * create; STATUS_CHANGED, CONFIDENCE_CHANGED, PRIORITY_CHANGED (only when the 10-point band
 * changes) and VALUE_CHANGED on patch — never for title, notes, owner or archived.
 */
@Service
@Transactional
public class TreeNodeWriteService {

    private static final Logger LOG = LoggerFactory.getLogger(TreeNodeWriteService.class);

    static final int DEFAULT_CONFIDENCE = 40;
    static final int DEFAULT_PRIORITY = 50;
    static final int DEFAULT_VALUE_RATING = 3;

    static final String F_TITLE = "title";
    static final String F_NOTES = "notes";
    static final String F_STATUS = "status";
    static final String F_CONFIDENCE = "confidence";
    static final String F_PRIORITY = "priority";
    static final String F_VALUE_RATING = "valueRating";
    static final String F_OWNER_LOGIN = "ownerLogin";
    static final String F_ARCHIVED = "archived";

    private static final Map<String, Set<TreeNodeType>> APPLICABLE = Map.of(
        F_TITLE,
        EnumSet.allOf(TreeNodeType.class),
        F_NOTES,
        EnumSet.allOf(TreeNodeType.class),
        F_STATUS,
        EnumSet.of(TreeNodeType.OPPORTUNITY, TreeNodeType.SOLUTION, TreeNodeType.ASSUMPTION),
        F_CONFIDENCE,
        EnumSet.of(TreeNodeType.ASSUMPTION),
        F_PRIORITY,
        EnumSet.of(TreeNodeType.OPPORTUNITY),
        F_VALUE_RATING,
        EnumSet.of(TreeNodeType.OPPORTUNITY),
        F_OWNER_LOGIN,
        EnumSet.of(TreeNodeType.ASSUMPTION),
        F_ARCHIVED,
        EnumSet.of(TreeNodeType.PRODUCT)
    );

    private final TeamAccessService teamAccessService;
    private final NodeHistoryRecorder historyRecorder;
    private final DefaultNodeLinks defaultNodeLinks;
    private final TreeNodeDtoAssembler dtoAssembler;
    private final TreeNodeCascadeService cascadeService;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TreeStructureLock structureLock;

    @PersistenceContext
    private EntityManager em;

    public TreeNodeWriteService(
        TeamAccessService teamAccessService,
        NodeHistoryRecorder historyRecorder,
        DefaultNodeLinks defaultNodeLinks,
        TreeNodeDtoAssembler dtoAssembler,
        TreeNodeCascadeService cascadeService,
        UserRepository userRepository,
        TeamMemberRepository teamMemberRepository,
        TreeStructureLock structureLock
    ) {
        this.teamAccessService = teamAccessService;
        this.historyRecorder = historyRecorder;
        this.defaultNodeLinks = defaultNodeLinks;
        this.dtoAssembler = dtoAssembler;
        this.cascadeService = cascadeService;
        this.userRepository = userRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.structureLock = structureLock;
    }

    // ---------------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------------

    /** Creates an outcome, opportunity, solution, assumption or evidence node under an existing parent. */
    public TreeNodeDTO create(CreateTreeNodeRequest request) {
        if (request == null) {
            throw new NodeWriteRuleException("Request body is required", TreeNodeRules.ENTITY_NAME, "unknowntype");
        }
        TreeNodeType type = TreeNodeRules.parseType(request.type(), "type");
        TreeNodeType parentType = TreeNodeRules.parseType(request.parentType(), "parentType");
        Long parentId = TreeNodeRules.wholeLong(request.parentId(), "parentId", "parentmissing");
        if (parentId == null) {
            throw new NodeWriteRuleException("parentId is required", TreeNodeRules.ENTITY_NAME, "parentmissing");
        }
        // Purely type-based, so checking it before access leaks nothing about existing ids.
        TreeNodeRules.requireAllowed(parentType, type);
        // Access before field validation (as PATCH does): a viewer never learns why their body is wrong.
        Long teamId = teamAccessService.requireEditNode(parentType, parentId);
        String title =
            request.title() == null || request.title().isBlank() ? TreeNodeRules.defaultTitle(type) : validTitle(type, request.title());
        LOG.debug("Create {} under {} {}", type, parentType, parentId);

        // sortOrder = max + 1 must be computed under the team's structure lock, or concurrent
        // creates under one parent (a double-click) all get the same sortOrder.
        structureLock.lockTeam(teamId);
        // Access was checked before the wait: the previous lock holder may have deleted the parent.
        structureLock.requireNode(parentType, parentId, teamId);
        Instant now = Instant.now();
        Object entity = switch (type) {
            case OUTCOME -> {
                Outcome o = new Outcome()
                    .title(title)
                    .product(em.find(Product.class, parentId))
                    .sortOrder(nextSortOrder("select max(x.sortOrder) from Outcome x where x.product.id = :pid", parentId))
                    .createdDate(now)
                    .lastModifiedDate(now);
                em.persist(o);
                yield o;
            }
            case OPPORTUNITY -> {
                Opportunity o = new Opportunity()
                    .title(title)
                    .status(OpportunityStatus.UNEXPLORED)
                    .priority(DEFAULT_PRIORITY)
                    .valuerating(DEFAULT_VALUE_RATING)
                    .createdDate(now)
                    .lastModifiedDate(now);
                if (parentType == TreeNodeType.OUTCOME) {
                    o.setOutcome(em.find(Outcome.class, parentId));
                    o.setSortOrder(
                        nextSortOrder("select max(x.sortOrder) from Opportunity x where x.outcome.id = :pid and x.parent is null", parentId)
                    );
                } else {
                    Opportunity parent = em.find(Opportunity.class, parentId);
                    o.setParent(parent);
                    o.setOutcome(parent.getOutcome());
                    o.setSortOrder(nextSortOrder("select max(x.sortOrder) from Opportunity x where x.parent.id = :pid", parentId));
                }
                em.persist(o);
                yield o;
            }
            case SOLUTION -> {
                Solution s = new Solution()
                    .title(title)
                    .status(SolutionStatus.CANDIDATE)
                    .opportunity(em.find(Opportunity.class, parentId))
                    .sortOrder(nextSortOrder("select max(x.sortOrder) from Solution x where x.opportunity.id = :pid", parentId))
                    .createdDate(now)
                    .lastModifiedDate(now);
                em.persist(s);
                yield s;
            }
            case ASSUMPTION -> {
                Assumption a = new Assumption()
                    .statement(title)
                    .status(AssumptionStatus.UNTESTED)
                    .confidence(DEFAULT_CONFIDENCE)
                    .solution(em.find(Solution.class, parentId))
                    .sortOrder(nextSortOrder("select max(x.sortOrder) from Assumption x where x.solution.id = :pid", parentId))
                    .createdDate(now)
                    .lastModifiedDate(now);
                em.persist(a);
                yield a;
            }
            case EVIDENCE -> {
                Evidence e = new Evidence().title(title).createdDate(now).lastModifiedDate(now);
                if (parentType == TreeNodeType.OPPORTUNITY) {
                    e.setOpportunity(em.find(Opportunity.class, parentId));
                    e.setSortOrder(nextSortOrder("select max(x.sortOrder) from Evidence x where x.opportunity.id = :pid", parentId));
                } else {
                    e.setAssumption(em.find(Assumption.class, parentId));
                    e.setSortOrder(nextSortOrder("select max(x.sortOrder) from Evidence x where x.assumption.id = :pid", parentId));
                }
                em.persist(e);
                yield e;
            }
            // Unreachable: requireAllowed rejects PRODUCT as a child type.
            case PRODUCT -> throw new NodeWriteRuleException(
                "Products are created from the Teams page",
                TreeNodeRules.ENTITY_NAME,
                "invalidparent"
            );
        };
        em.flush();
        Long id = (Long) em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
        defaultNodeLinks.addDefaults(entity);
        historyRecorder.record(type, id, HistoryEventType.CREATED, "Node created as " + TreeNodeRules.label(type));
        em.flush();
        return dtoAssembler.toDto(type, id);
    }

    private int nextSortOrder(String maxQuery, Long parentId) {
        Integer max = em.createQuery(maxQuery, Integer.class).setParameter("pid", parentId).getSingleResult();
        return max == null ? 0 : max + 1;
    }

    // ---------------------------------------------------------------------
    // Patch (JSON merge patch semantics: absent = unchanged, null = clear where allowed)
    // ---------------------------------------------------------------------

    /**
     * Applies a partial update. Accepted fields: {@code title} (product name / assumption
     * statement), {@code notes}, {@code status}, {@code confidence}, {@code priority},
     * {@code valueRating}, {@code ownerLogin}, {@code archived} — each only on the types it applies to.
     */
    public TreeNodeDTO patch(TreeNodeType type, Long id, Map<String, Object> body) {
        Long teamId = teamAccessService.requireEditNode(type, id);
        Map<String, Object> fields = body == null ? Map.of() : body;
        for (String field : fields.keySet()) {
            Set<TreeNodeType> types = APPLICABLE.get(field);
            if (types == null) {
                throw new NodeWriteRuleException("Unknown field: " + field, TreeNodeRules.ENTITY_NAME, "unknownfield");
            }
            if (!types.contains(type)) {
                throw new NodeWriteRuleException(
                    "Field '" + field + "' does not apply to a " + TreeNodeRules.label(type),
                    TreeNodeRules.ENTITY_NAME,
                    "fieldnotapplicable"
                );
            }
        }
        LOG.debug("Patch {} {} fields {}", type, id, fields.keySet());
        if (fields.isEmpty()) {
            return dtoAssembler.toDto(type, id);
        }
        // Under the team's structure lock, like every other write that can race a cascade delete:
        // a patch (and the history row it records) never lands on a node deleted meanwhile — that
        // is a 409 concurrencyFailure, not an NPE / stale-row 500.
        structureLock.lockTeam(teamId);
        structureLock.requireNode(type, id, teamId);

        Instant now = Instant.now();
        switch (type) {
            case PRODUCT -> {
                Product p = em.find(Product.class, id);
                if (fields.containsKey(F_TITLE)) {
                    p.setName(validTitle(type, fields.get(F_TITLE)));
                }
                if (fields.containsKey(F_NOTES)) {
                    p.setDescription(notes(fields.get(F_NOTES)));
                }
                if (fields.containsKey(F_ARCHIVED)) {
                    p.setArchived(bool(fields.get(F_ARCHIVED), "invalidarchived"));
                }
            }
            case OUTCOME -> {
                Outcome o = em.find(Outcome.class, id);
                if (fields.containsKey(F_TITLE)) {
                    o.setTitle(validTitle(type, fields.get(F_TITLE)));
                }
                if (fields.containsKey(F_NOTES)) {
                    o.setDescription(notes(fields.get(F_NOTES)));
                }
                o.setLastModifiedDate(now);
            }
            case OPPORTUNITY -> {
                Opportunity o = em.find(Opportunity.class, id);
                if (fields.containsKey(F_TITLE)) {
                    o.setTitle(validTitle(type, fields.get(F_TITLE)));
                }
                if (fields.containsKey(F_NOTES)) {
                    o.setDescription(notes(fields.get(F_NOTES)));
                }
                if (fields.containsKey(F_STATUS)) {
                    OpportunityStatus next = (OpportunityStatus) status(type, fields.get(F_STATUS));
                    OpportunityStatus before = o.getStatus();
                    o.setStatus(next);
                    recordStatus(type, id, before, next);
                }
                if (fields.containsKey(F_PRIORITY)) {
                    int next = integer(fields.get(F_PRIORITY), 1, 100, "invalidpriority");
                    Integer before = o.getPriority();
                    o.setPriority(next);
                    if (NodeHistoryRecorder.isPriorityBandChange(before, next)) {
                        historyRecorder.record(
                            type,
                            id,
                            HistoryEventType.PRIORITY_CHANGED,
                            "Priority set to " + TreeNodeRules.priorityLabel(next).toLowerCase(Locale.ROOT)
                        );
                    }
                }
                if (fields.containsKey(F_VALUE_RATING)) {
                    int next = integer(fields.get(F_VALUE_RATING), 1, 5, "invalidvaluerating");
                    Integer before = o.getValuerating();
                    o.setValuerating(next);
                    if (!Objects.equals(before, next)) {
                        historyRecorder.record(type, id, HistoryEventType.VALUE_CHANGED, "Value set to " + "$".repeat(next));
                    }
                }
                o.setLastModifiedDate(now);
            }
            case SOLUTION -> {
                Solution s = em.find(Solution.class, id);
                if (fields.containsKey(F_TITLE)) {
                    s.setTitle(validTitle(type, fields.get(F_TITLE)));
                }
                if (fields.containsKey(F_NOTES)) {
                    s.setDescription(notes(fields.get(F_NOTES)));
                }
                if (fields.containsKey(F_STATUS)) {
                    SolutionStatus next = (SolutionStatus) status(type, fields.get(F_STATUS));
                    SolutionStatus before = s.getStatus();
                    s.setStatus(next);
                    recordStatus(type, id, before, next);
                }
                s.setLastModifiedDate(now);
            }
            case ASSUMPTION -> {
                Assumption a = em.find(Assumption.class, id);
                if (fields.containsKey(F_TITLE)) {
                    a.setStatement(validTitle(type, fields.get(F_TITLE)));
                }
                if (fields.containsKey(F_NOTES)) {
                    a.setDescription(notes(fields.get(F_NOTES)));
                }
                if (fields.containsKey(F_STATUS)) {
                    AssumptionStatus next = (AssumptionStatus) status(type, fields.get(F_STATUS));
                    AssumptionStatus before = a.getStatus();
                    a.setStatus(next);
                    recordStatus(type, id, before, next);
                }
                if (fields.containsKey(F_CONFIDENCE)) {
                    int next = integer(fields.get(F_CONFIDENCE), 0, 100, "invalidconfidence");
                    Integer before = a.getConfidence();
                    a.setConfidence(next);
                    if (!Objects.equals(before, next)) {
                        historyRecorder.record(type, id, HistoryEventType.CONFIDENCE_CHANGED, "Confidence set to " + next + "%");
                    }
                }
                if (fields.containsKey(F_OWNER_LOGIN)) {
                    a.setOwner(owner(teamId, fields.get(F_OWNER_LOGIN)));
                }
                a.setLastModifiedDate(now);
            }
            case EVIDENCE -> {
                Evidence e = em.find(Evidence.class, id);
                if (fields.containsKey(F_TITLE)) {
                    e.setTitle(validTitle(type, fields.get(F_TITLE)));
                }
                if (fields.containsKey(F_NOTES)) {
                    e.setDescription(notes(fields.get(F_NOTES)));
                }
                e.setLastModifiedDate(now);
            }
        }
        em.flush();
        return dtoAssembler.toDto(type, id);
    }

    // ---------------------------------------------------------------------
    // Delete
    // ---------------------------------------------------------------------

    /** Deletes the node and its whole subtree (see {@link TreeNodeCascadeService}). */
    public void delete(TreeNodeType type, Long id) {
        cascadeService.deleteNode(type, id);
    }

    // ---------------------------------------------------------------------
    // Value helpers — every invalid value is a 400 with a field-specific key.
    // ---------------------------------------------------------------------

    private void recordStatus(TreeNodeType type, Long id, Enum<?> before, Enum<?> next) {
        if (before != next) {
            historyRecorder.record(type, id, HistoryEventType.STATUS_CHANGED, TreeNodeRules.statusChangedSummary(next));
        }
    }

    static String validTitle(TreeNodeType type, Object value) {
        if (!(value instanceof String s)) {
            throw new NodeWriteRuleException("title must be a string", TreeNodeRules.ENTITY_NAME, "invalidtitle");
        }
        String title = s.trim();
        int max = TreeNodeRules.maxTitleLength(type);
        if (title.length() < TreeNodeRules.MIN_TITLE_LENGTH || title.length() > max) {
            throw new NodeWriteRuleException(
                "title must be between " + TreeNodeRules.MIN_TITLE_LENGTH + " and " + max + " characters",
                TreeNodeRules.ENTITY_NAME,
                "invalidtitle"
            );
        }
        return title;
    }

    private static String notes(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String s)) {
            throw new NodeWriteRuleException("notes must be a string", TreeNodeRules.ENTITY_NAME, "invalidnotes");
        }
        return s;
    }

    private static Enum<?> status(TreeNodeType type, Object value) {
        if (!(value instanceof String s)) {
            throw new NodeWriteRuleException("status must be a string", TreeNodeRules.ENTITY_NAME, "invalidstatus");
        }
        return TreeNodeRules.parseStatus(type, s);
    }

    /**
     * A whole number in {@code [min, max]}. The value is compared exactly (as a BigDecimal) so
     * JSON numbers outside the long range — Jackson hands those over as BigInteger — are
     * rejected instead of wrapping (e.g. 2^64 + 50 is not 50).
     */
    static int integer(Object value, int min, int max, String errorKey) {
        BigDecimal exact = TreeNodeRules.exactNumber(value);
        if (
            exact != null &&
            exact.stripTrailingZeros().scale() <= 0 &&
            exact.compareTo(BigDecimal.valueOf(min)) >= 0 &&
            exact.compareTo(BigDecimal.valueOf(max)) <= 0
        ) {
            return exact.intValueExact();
        }
        throw new NodeWriteRuleException(
            "Value must be a whole number between " + min + " and " + max,
            TreeNodeRules.ENTITY_NAME,
            errorKey
        );
    }

    private static Boolean bool(Object value, String errorKey) {
        if (value instanceof Boolean b) {
            return b;
        }
        throw new NodeWriteRuleException("Value must be true or false", TreeNodeRules.ENTITY_NAME, errorKey);
    }

    private User owner(Long teamId, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String login && !login.isBlank()) {
            User user = userRepository.findOneByLogin(login.trim()).orElse(null);
            if (user != null && teamMemberRepository.existsByTeamIdAndUserId(teamId, user.getId())) {
                return user;
            }
        }
        throw new NodeWriteRuleException("The owner must be a member of the node's team", TreeNodeRules.ENTITY_NAME, "ownernotmember");
    }
}
