package com.opportunity.tree.service;

import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.MeetingTranscript;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.MeetingTranscriptSource;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.MeetingTranscriptRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.security.SecurityUtils;
import com.opportunity.tree.service.dto.tree.TreeTranscriptDTO;
import com.opportunity.tree.service.dto.tree.TreeTranscriptMetaDTO;
import com.opportunity.tree.service.dto.tree.TreeTranscriptWriteDTO;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Team-scoped meeting transcript CRUD (Epic 12 / MTRANS-002).
 *
 * <p>Access rules — enforced through {@link TeamAccessService} on every entry point:
 * <ul>
 *   <li>Read (list by node, list by team, single-body fetch): any member of the owning team.</li>
 *   <li>Write (create, edit, delete): OWNER or EDITOR of the owning team. Viewers, non-members
 *       and admins-without-membership are rejected with the same 403 as a missing id.</li>
 * </ul>
 *
 * <p>A transcript is bound to exactly one tree node for life. On create, the request must set
 * exactly one of the six node id fields, and that node must belong to the caller's team; anything
 * else is a 400 with a stable error key ({@code error.transcriptnodeinvalid} /
 * {@code error.transcripttitleinvalid} / {@code error.transcriptbodyinvalid} /
 * {@code error.transcriptdatemissing}). On update the node fields are ignored: editing cannot
 * move a transcript to another node (NFR concern, criterion 4).
 *
 * <p>Author and {@code createdDate} are server-set from the security context and clock; editing
 * refreshes {@code editedDate}. The body has a documented 1 MiB UTF-8 ceiling
 * ({@link #BODY_MAX_BYTES}); blank bodies are rejected.
 *
 * <p>List responses carry metadata only ({@link TreeTranscriptMetaDTO}); the full body only ever
 * leaves the server through {@link #getTranscript(Long)}. Nothing about a transcript is published
 * over WebSocket — NFR-022. Logs never carry the body: the service logs ids only.
 */
@Service
@Transactional
public class TreeMeetingTranscriptService {

    /** Stable entity name reported in {@link NodeWriteRuleException} problem details. */
    public static final String ENTITY_NAME = "meetingTranscript";

    /** Documented pasted-body ceiling — 1 MiB of UTF-8 bytes, per Epic 12 §10 (transcript size). */
    public static final int BODY_MAX_BYTES = 1024 * 1024;

    static final int TITLE_MIN = 2;
    static final int TITLE_MAX = 200;
    static final int ATTENDEES_MAX = 500;
    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 100;

    private final TeamAccessService teamAccessService;
    private final TreeStructureLock structureLock;
    private final NodeHistoryRecorder historyRecorder;
    private final MeetingTranscriptRepository transcriptRepository;
    private final UserRepository userRepository;
    private final EntityManager em;

    public TreeMeetingTranscriptService(
        TeamAccessService teamAccessService,
        TreeStructureLock structureLock,
        NodeHistoryRecorder historyRecorder,
        MeetingTranscriptRepository transcriptRepository,
        UserRepository userRepository,
        EntityManager em
    ) {
        this.teamAccessService = teamAccessService;
        this.structureLock = structureLock;
        this.historyRecorder = historyRecorder;
        this.transcriptRepository = transcriptRepository;
        this.userRepository = userRepository;
        this.em = em;
    }

    /** Metadata list for one node, newest first (paginated). Body is not returned. */
    @Transactional(readOnly = true)
    public Page<TreeTranscriptMetaDTO> listByNode(TreeNodeType type, Long nodeId, Pageable pageable) {
        teamAccessService.requireReadNode(type, nodeId);
        Pageable paging = pagingNewestFirst(pageable);
        Page<MeetingTranscript> page = switch (type) {
            case PRODUCT -> transcriptRepository.findAllByProductId(nodeId, paging);
            case OUTCOME -> transcriptRepository.findAllByOutcomeId(nodeId, paging);
            case OPPORTUNITY -> transcriptRepository.findAllByOpportunityId(nodeId, paging);
            case SOLUTION -> transcriptRepository.findAllBySolutionId(nodeId, paging);
            case ASSUMPTION -> transcriptRepository.findAllByAssumptionId(nodeId, paging);
            case EVIDENCE -> transcriptRepository.findAllByEvidenceId(nodeId, paging);
        };
        return page.map(TreeMeetingTranscriptService::toMetaDto);
    }

    /** Metadata list for every node in a team, newest first (paginated). Body is not returned. */
    @Transactional(readOnly = true)
    public Page<TreeTranscriptMetaDTO> listByTeam(Long teamId, Pageable pageable) {
        teamAccessService.requireReadTeam(teamId);
        return transcriptRepository.findAllByTeamId(teamId, pagingNewestFirst(pageable)).map(TreeMeetingTranscriptService::toMetaDto);
    }

    /** Full transcript body — the ONLY endpoint that returns the body (NFR-022 / NFR-024). */
    @Transactional(readOnly = true)
    public TreeTranscriptDTO getTranscript(Long transcriptId) {
        teamAccessService.requireReadTranscript(transcriptId);
        MeetingTranscript t = transcriptRepository.findOneWithEagerRelationships(transcriptId).orElseThrow(TeamAccessDeniedException::new);
        return toDto(t);
    }

    /** Creates a new transcript on exactly one node of the caller's team. */
    public TreeTranscriptDTO create(TreeTranscriptWriteDTO request) {
        if (request == null) {
            throw new NodeWriteRuleException("Request body is required", ENTITY_NAME, "transcriptnodeinvalid");
        }
        NodeSelection node = exactlyOneNode(request);
        Long teamId = teamAccessService.requireEditNode(node.type(), node.id());
        String title = validTitle(request.title());
        LocalDate meetingDate = validMeetingDate(request.meetingDate());
        String attendees = validAttendees(request.attendees());
        String body = validBody(request.body());
        MeetingTranscriptSource source = request.source() == null ? MeetingTranscriptSource.PASTED : request.source();
        structureLock.lockTeam(teamId);
        structureLock.requireNode(node.type(), node.id(), teamId);
        User author = SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneByLogin)
            .orElseThrow(TeamAccessDeniedException::new);

        MeetingTranscript t = new MeetingTranscript()
            .title(title)
            .meetingDate(meetingDate)
            .attendees(attendees)
            .body(body)
            .source(source)
            .createdDate(Instant.now())
            .author(author);
        attach(t, node);
        t = transcriptRepository.save(t);
        historyRecorder.record(node.type(), node.id(), HistoryEventType.TRANSCRIPT_ADDED, "Transcript added");
        em.flush();
        return toDto(t);
    }

    /** Edits a transcript in place. The node relationship is immutable — attempts to move it are ignored. */
    public TreeTranscriptDTO update(Long transcriptId, TreeTranscriptWriteDTO request) {
        if (request == null) {
            throw new NodeWriteRuleException("Request body is required", ENTITY_NAME, "transcripttitleinvalid");
        }
        TreeNodeRef node = teamAccessService.requireEditTranscript(transcriptId);
        String title = validTitle(request.title());
        LocalDate meetingDate = validMeetingDate(request.meetingDate());
        String attendees = validAttendees(request.attendees());
        String body = validBody(request.body());
        Long teamId = teamAccessService.teamIdForNode(node.type(), node.id()).orElseThrow(TeamAccessDeniedException::new);
        structureLock.lockTeam(teamId);
        structureLock.requireNode(node.type(), node.id(), teamId);
        MeetingTranscript t = transcriptRepository.findOneWithEagerRelationships(transcriptId).orElseThrow(TeamAccessDeniedException::new);
        t.setTitle(title);
        t.setMeetingDate(meetingDate);
        t.setAttendees(attendees);
        t.setBody(body);
        t.setEditedDate(Instant.now());
        t = transcriptRepository.save(t);
        em.flush();
        return toDto(t);
    }

    /** Deletes a transcript, leaving the node itself intact. History: TRANSCRIPT_DELETED "Transcript deleted". */
    public void delete(Long transcriptId) {
        TreeNodeRef node = teamAccessService.requireEditTranscript(transcriptId);
        Long teamId = teamAccessService.teamIdForNode(node.type(), node.id()).orElseThrow(TeamAccessDeniedException::new);
        structureLock.lockTeam(teamId);
        structureLock.requireNode(node.type(), node.id(), teamId);
        MeetingTranscript t = transcriptRepository.findById(transcriptId).orElseThrow(TeamAccessDeniedException::new);
        transcriptRepository.delete(t);
        historyRecorder.record(node.type(), node.id(), HistoryEventType.TRANSCRIPT_DELETED, "Transcript deleted");
        em.flush();
    }

    // ---------------------------------------------------------------------

    private Pageable pagingNewestFirst(Pageable pageable) {
        Sort sort = Sort.by(Sort.Order.desc("createdDate"), Sort.Order.desc("id"));
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, DEFAULT_PAGE_SIZE, sort);
        }
        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }

    private NodeSelection exactlyOneNode(TreeTranscriptWriteDTO request) {
        Map<TreeNodeType, Long> set = new EnumMap<>(TreeNodeType.class);
        putIfSet(set, TreeNodeType.PRODUCT, request.productId());
        putIfSet(set, TreeNodeType.OUTCOME, request.outcomeId());
        putIfSet(set, TreeNodeType.OPPORTUNITY, request.opportunityId());
        putIfSet(set, TreeNodeType.SOLUTION, request.solutionId());
        putIfSet(set, TreeNodeType.ASSUMPTION, request.assumptionId());
        putIfSet(set, TreeNodeType.EVIDENCE, request.evidenceId());
        if (set.size() != 1) {
            throw new NodeWriteRuleException("A transcript must reference exactly one node", ENTITY_NAME, "transcriptnodeinvalid");
        }
        Map.Entry<TreeNodeType, Long> only = set.entrySet().iterator().next();
        return new NodeSelection(only.getKey(), only.getValue());
    }

    private static void putIfSet(Map<TreeNodeType, Long> set, TreeNodeType type, Long value) {
        if (value != null) {
            set.put(type, value);
        }
    }

    private void attach(MeetingTranscript t, NodeSelection node) {
        switch (node.type()) {
            case PRODUCT -> t.setProduct(em.getReference(Product.class, node.id()));
            case OUTCOME -> t.setOutcome(em.getReference(Outcome.class, node.id()));
            case OPPORTUNITY -> t.setOpportunity(em.getReference(Opportunity.class, node.id()));
            case SOLUTION -> t.setSolution(em.getReference(Solution.class, node.id()));
            case ASSUMPTION -> t.setAssumption(em.getReference(Assumption.class, node.id()));
            case EVIDENCE -> t.setEvidence(em.getReference(Evidence.class, node.id()));
        }
    }

    private static String validTitle(String raw) {
        String title = raw == null ? "" : raw.strip();
        if (title.length() < TITLE_MIN || title.length() > TITLE_MAX) {
            throw new NodeWriteRuleException(
                "Title must be " + TITLE_MIN + "-" + TITLE_MAX + " characters",
                ENTITY_NAME,
                "transcripttitleinvalid"
            );
        }
        return title;
    }

    private static LocalDate validMeetingDate(LocalDate raw) {
        if (raw == null) {
            throw new NodeWriteRuleException("Meeting date is required", ENTITY_NAME, "transcriptdatemissing");
        }
        return raw;
    }

    private static String validAttendees(String raw) {
        if (raw == null) {
            return null;
        }
        String attendees = raw.strip();
        if (attendees.isEmpty()) {
            return null;
        }
        if (attendees.length() > ATTENDEES_MAX) {
            throw new NodeWriteRuleException(
                "Attendees must be at most " + ATTENDEES_MAX + " characters",
                ENTITY_NAME,
                "transcriptattendeesinvalid"
            );
        }
        return attendees;
    }

    private static String validBody(String raw) {
        String body = raw == null ? "" : raw.strip();
        if (body.isEmpty()) {
            throw new NodeWriteRuleException("Transcript body cannot be blank", ENTITY_NAME, "transcriptbodyinvalid");
        }
        if (body.getBytes(StandardCharsets.UTF_8).length > BODY_MAX_BYTES) {
            throw new NodeWriteRuleException(
                "Transcript body cannot exceed " + BODY_MAX_BYTES + " bytes of UTF-8",
                ENTITY_NAME,
                "transcriptbodytoolarge"
            );
        }
        return body;
    }

    private static TreeTranscriptMetaDTO toMetaDto(MeetingTranscript t) {
        NodeSelection node = nodeOf(t);
        User author = t.getAuthor();
        return new TreeTranscriptMetaDTO(
            t.getId(),
            t.getTitle(),
            t.getMeetingDate(),
            t.getAttendees(),
            t.getSource(),
            node == null ? null : node.type(),
            node == null ? null : node.id(),
            node == null ? null : TreeNodeRef.key(node.type(), node.id()),
            author == null ? null : author.getLogin(),
            author == null ? null : TeamTreeService.initials(author),
            fullName(author),
            t.getCreatedDate(),
            t.getEditedDate()
        );
    }

    private static TreeTranscriptDTO toDto(MeetingTranscript t) {
        NodeSelection node = nodeOf(t);
        User author = t.getAuthor();
        return new TreeTranscriptDTO(
            t.getId(),
            t.getTitle(),
            t.getMeetingDate(),
            t.getAttendees(),
            t.getBody(),
            t.getSource(),
            node == null ? null : node.type(),
            node == null ? null : node.id(),
            node == null ? null : TreeNodeRef.key(node.type(), node.id()),
            author == null ? null : author.getLogin(),
            author == null ? null : TeamTreeService.initials(author),
            fullName(author),
            t.getCreatedDate(),
            t.getEditedDate()
        );
    }

    private static NodeSelection nodeOf(MeetingTranscript t) {
        if (t.getProduct() != null) {
            return new NodeSelection(TreeNodeType.PRODUCT, t.getProduct().getId());
        }
        if (t.getOutcome() != null) {
            return new NodeSelection(TreeNodeType.OUTCOME, t.getOutcome().getId());
        }
        if (t.getOpportunity() != null) {
            return new NodeSelection(TreeNodeType.OPPORTUNITY, t.getOpportunity().getId());
        }
        if (t.getSolution() != null) {
            return new NodeSelection(TreeNodeType.SOLUTION, t.getSolution().getId());
        }
        if (t.getAssumption() != null) {
            return new NodeSelection(TreeNodeType.ASSUMPTION, t.getAssumption().getId());
        }
        if (t.getEvidence() != null) {
            return new NodeSelection(TreeNodeType.EVIDENCE, t.getEvidence().getId());
        }
        return null;
    }

    private static String fullName(User author) {
        if (author == null) {
            return null;
        }
        String name = Stream.of(author.getFirstName(), author.getLastName())
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .reduce((a, b) -> a + " " + b)
            .orElse(null);
        return name == null ? author.getLogin() : name;
    }

    private record NodeSelection(TreeNodeType type, Long id) {}
}
