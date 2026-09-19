package com.opportunity.tree.service;

import com.opportunity.tree.domain.NodeHistory;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.TreeCollaborationRepository;
import com.opportunity.tree.service.dto.tree.TreeHistoryEntryDTO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read side of the per-node changelog (FR-H1): every node type except PRODUCT,
 * newest first, readable by any team member. Writing happens only through
 * {@link NodeHistoryRecorder}.
 */
@Service
@Transactional(readOnly = true)
public class TreeNodeHistoryService {

    private final TeamAccessService teamAccessService;
    private final TreeCollaborationRepository collaborationRepository;

    public TreeNodeHistoryService(TeamAccessService teamAccessService, TreeCollaborationRepository collaborationRepository) {
        this.teamAccessService = teamAccessService;
        this.collaborationRepository = collaborationRepository;
    }

    public List<TreeHistoryEntryDTO> getHistory(TreeNodeType type, Long nodeId) {
        if (type == TreeNodeType.PRODUCT) {
            throw new NodeWriteRuleException("Products have no history", "nodeHistory", "historynotsupported");
        }
        teamAccessService.requireReadNode(type, nodeId);
        return collaborationRepository.findHistoryOfNode(type, nodeId).stream().map(TreeNodeHistoryService::toDto).toList();
    }

    private static TreeHistoryEntryDTO toDto(NodeHistory h) {
        User author = h.getAuthor();
        return new TreeHistoryEntryDTO(
            h.getId(),
            h.getEventType(),
            h.getSummary(),
            author == null ? null : author.getLogin(),
            author == null ? null : TeamTreeService.initials(author),
            h.getCreatedDate()
        );
    }
}
