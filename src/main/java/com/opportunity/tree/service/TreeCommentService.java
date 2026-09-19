package com.opportunity.tree.service;

import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Comment;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.CommentRepository;
import com.opportunity.tree.repository.TreeCollaborationRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.security.SecurityUtils;
import com.opportunity.tree.service.dto.tree.TreeCommentDTO;
import com.opportunity.tree.service.dto.tree.TreeCommentWriteDTO;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Per-node chat (FR-M1..M3) on every node type except PRODUCT.
 *
 * <ul>
 *   <li>Read: any team member.</li>
 *   <li>Post: OWNER or EDITOR (viewers are read-only, including chat).</li>
 *   <li>Edit / delete: the comment's author only, and only while still OWNER or EDITOR.</li>
 * </ul>
 * Posting and deleting are recorded in the node's history; editing is not.
 *
 * <p>Every write takes the team's {@link TreeStructureLock} (after the access check, before loading
 * anything) and re-checks that the node / comment still exists: a comment inserted after a
 * concurrent cascade delete collected the node's comments would otherwise break that delete with a
 * foreign-key violation, and an edit could land on a row deleted meanwhile. A node or comment
 * deleted by the previous lock holder is a 409 {@code error.concurrencyFailure}.
 *
 * <p>Chat is flat by design: there is no threading in the model, so every comment on the node is
 * returned as one thread, oldest first.
 */
@Service
@Transactional
public class TreeCommentService {

    static final String ENTITY_NAME = "comment";
    static final int BODY_MAX = 10_000;

    private final TeamAccessService teamAccessService;
    private final TreeStructureLock structureLock;
    private final NodeHistoryRecorder historyRecorder;
    private final CommentRepository commentRepository;
    private final TreeCollaborationRepository collaborationRepository;
    private final UserRepository userRepository;
    private final EntityManager em;

    public TreeCommentService(
        TeamAccessService teamAccessService,
        TreeStructureLock structureLock,
        NodeHistoryRecorder historyRecorder,
        CommentRepository commentRepository,
        TreeCollaborationRepository collaborationRepository,
        UserRepository userRepository,
        EntityManager em
    ) {
        this.teamAccessService = teamAccessService;
        this.structureLock = structureLock;
        this.historyRecorder = historyRecorder;
        this.commentRepository = commentRepository;
        this.collaborationRepository = collaborationRepository;
        this.userRepository = userRepository;
        this.em = em;
    }

    /** The node's thread, oldest first. */
    @Transactional(readOnly = true)
    public List<TreeCommentDTO> getComments(TreeNodeType type, Long nodeId) {
        requireChatType(type);
        teamAccessService.requireReadNode(type, nodeId);
        List<Comment> comments = switch (type) {
            case OUTCOME -> collaborationRepository.findCommentsOfOutcome(nodeId);
            case OPPORTUNITY -> collaborationRepository.findCommentsOfOpportunity(nodeId);
            case SOLUTION -> collaborationRepository.findCommentsOfSolution(nodeId);
            case ASSUMPTION -> collaborationRepository.findCommentsOfAssumption(nodeId);
            case EVIDENCE -> collaborationRepository.findCommentsOfEvidence(nodeId);
            case PRODUCT -> throw new IllegalStateException("unreachable");
        };
        String me = SecurityUtils.getCurrentUserLogin().orElse(null);
        return comments
            .stream()
            .map(c -> toDto(c, me))
            .collect(Collectors.toList());
    }

    /** Posts a message as the current user. History: COMMENT_ADDED "Comment added". */
    public TreeCommentDTO addComment(TreeNodeType type, Long nodeId, TreeCommentWriteDTO request) {
        requireChatType(type);
        Long teamId = teamAccessService.requireEditNode(type, nodeId);
        String body = validBody(request == null ? null : request.body());
        structureLock.lockTeam(teamId);
        structureLock.requireNode(type, nodeId, teamId);
        User author = SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneByLogin)
            .orElseThrow(TeamAccessDeniedException::new);

        Comment comment = new Comment().body(body).createdDate(Instant.now()).author(author);
        switch (type) {
            case OUTCOME -> comment.setOutcome(em.getReference(Outcome.class, nodeId));
            case OPPORTUNITY -> comment.setOpportunity(em.getReference(Opportunity.class, nodeId));
            case SOLUTION -> comment.setSolution(em.getReference(Solution.class, nodeId));
            case ASSUMPTION -> comment.setAssumption(em.getReference(Assumption.class, nodeId));
            case EVIDENCE -> comment.setEvidence(em.getReference(Evidence.class, nodeId));
            case PRODUCT -> throw new IllegalStateException("unreachable");
        }
        comment = commentRepository.save(comment);
        historyRecorder.record(type, nodeId, HistoryEventType.COMMENT_ADDED, "Comment added");
        return toDto(comment, author.getLogin());
    }

    /** Edits the current user's own message and stamps {@code editedDate}. Writes no history. */
    public TreeCommentDTO updateComment(Long commentId, TreeCommentWriteDTO request) {
        TreeNodeRef node = teamAccessService.requireEditComment(commentId);
        String body = validBody(request == null ? null : request.body());
        lockNodeOfComment(node, commentId);
        Comment comment = requireOwnComment(commentId);
        comment.setBody(body);
        comment.setEditedDate(Instant.now());
        comment = commentRepository.save(comment);
        return toDto(comment, comment.getAuthor().getLogin());
    }

    /** Deletes the current user's own message. History: COMMENT_DELETED "Comment deleted". */
    public void deleteComment(Long commentId) {
        TreeNodeRef node = teamAccessService.requireEditComment(commentId);
        lockNodeOfComment(node, commentId);
        Comment comment = requireOwnComment(commentId);
        commentRepository.delete(comment);
        historyRecorder.record(node.type(), node.id(), HistoryEventType.COMMENT_DELETED, "Comment deleted");
    }

    /**
     * Takes the structure lock of the comment's team, then re-checks that the node and the comment
     * survived the previous lock holder (409 otherwise).
     */
    private void lockNodeOfComment(TreeNodeRef node, Long commentId) {
        Long teamId = teamAccessService.teamIdForNode(node.type(), node.id()).orElseThrow(TeamAccessDeniedException::new);
        structureLock.lockTeam(teamId);
        structureLock.requireNode(node.type(), node.id(), teamId);
        if (teamAccessService.nodeOfComment(commentId).isEmpty()) {
            throw new ConcurrencyFailureException("Comment " + commentId + " was deleted by a concurrent request");
        }
    }

    private Comment requireOwnComment(Long commentId) {
        Comment comment = collaborationRepository
            .findCommentWithAuthor(commentId)
            .stream()
            .findFirst()
            .orElseThrow(TeamAccessDeniedException::new);
        String me = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (me == null || !me.equals(comment.getAuthor().getLogin())) {
            throw new TeamAccessDeniedException("Only the author can change this comment");
        }
        return comment;
    }

    static void requireChatType(TreeNodeType type) {
        if (type == TreeNodeType.PRODUCT) {
            throw new NodeWriteRuleException("Products have no chat", ENTITY_NAME, "chatnotsupported");
        }
    }

    private static String validBody(String raw) {
        String body = raw == null ? "" : raw.strip();
        if (body.isEmpty() || body.length() > BODY_MAX) {
            throw new NodeWriteRuleException("Message must be 1-" + BODY_MAX + " characters", ENTITY_NAME, "commentbodyinvalid");
        }
        return body;
    }

    private static TreeCommentDTO toDto(Comment c, String currentLogin) {
        User author = c.getAuthor();
        String login = author == null ? null : author.getLogin();
        String name =
            author == null
                ? null
                : Stream.of(author.getFirstName(), author.getLastName())
                      .filter(Objects::nonNull)
                      .map(String::trim)
                      .filter(s -> !s.isEmpty())
                      .collect(Collectors.joining(" "));
        if (name != null && name.isEmpty()) {
            name = login;
        }
        return new TreeCommentDTO(
            c.getId(),
            c.getBody(),
            login,
            author == null ? null : TeamTreeService.initials(author),
            name,
            c.getCreatedDate(),
            c.getEditedDate(),
            login != null && login.equals(currentLogin)
        );
    }
}
