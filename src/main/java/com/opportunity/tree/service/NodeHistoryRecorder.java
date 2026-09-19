package com.opportunity.tree.service;

import com.opportunity.tree.domain.NodeHistory;
import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.NodeHistoryRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.security.SecurityUtils;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Appends rows to the per-node changelog ({@link NodeHistory}). History is
 * written server-side only, by the tree write services, inside the caller's
 * transaction — so a rolled-back write leaves no history behind.
 *
 * <p>The author is the current user (resolved from the security context, never
 * from the client) and the timestamp is "now". Summaries longer than the
 * column allows are truncated with an ellipsis.
 */
@Service
@Transactional
public class NodeHistoryRecorder {

    static final int MAX_SUMMARY_LENGTH = 500;

    private final NodeHistoryRepository nodeHistoryRepository;
    private final UserRepository userRepository;

    public NodeHistoryRecorder(NodeHistoryRepository nodeHistoryRepository, UserRepository userRepository) {
        this.nodeHistoryRepository = nodeHistoryRepository;
        this.userRepository = userRepository;
    }

    /**
     * Record one history event for a node.
     *
     * @param type    node type.
     * @param nodeId  node id.
     * @param event   event type.
     * @param summary human-readable summary in the prototype's wording, e.g. {@code Status → Validated}.
     * @return the saved row.
     */
    public NodeHistory record(TreeNodeType type, Long nodeId, HistoryEventType event, String summary) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(event, "event");
        NodeHistory history = new NodeHistory()
            .nodeType(type)
            .nodeId(nodeId)
            .eventType(event)
            .summary(truncate(summary))
            .createdDate(Instant.now());
        SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin).ifPresent(history::setAuthor);
        return nodeHistoryRepository.save(history);
    }

    /** The priority band shown on the canvas: {@code round(priority / 10)}, halves rounding up. */
    public static int priorityBand(int priority) {
        return Math.round(priority / 10f);
    }

    /**
     * Priority changes are only recorded when the visible band changes
     * ({@code round(p / 10)}), so dragging the slider within a band is silent.
     */
    public static boolean isPriorityBandChange(Integer before, Integer after) {
        if (before == null || after == null) {
            return !Objects.equals(before, after);
        }
        return priorityBand(before) != priorityBand(after);
    }

    private static String truncate(String summary) {
        String s = summary == null ? "" : summary;
        if (s.length() <= MAX_SUMMARY_LENGTH) {
            return s;
        }
        return s.substring(0, MAX_SUMMARY_LENGTH - 1) + "…";
    }
}
