package com.opportunity.tree.service;

import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.NodeLink;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.NodeLinkRepository;
import com.opportunity.tree.repository.TreeCollaborationRepository;
import com.opportunity.tree.service.dto.tree.TreeLinkDTO;
import com.opportunity.tree.service.dto.tree.TreeLinkWriteDTO;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Name + URL links on any of the six tree node types (FR-D4). Read = any team
 * member (links come with the tree read); add / edit / remove = OWNER or EDITOR.
 * Adding and removing a link is recorded in the node's history; renaming or
 * re-pointing one is not.
 */
@Service
@Transactional
public class TreeNodeLinkService {

    static final String ENTITY_NAME = "nodeLink";
    static final int NAME_MAX = 100;
    static final int URL_MAX = 2000;
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://.+");

    private final TeamAccessService teamAccessService;
    private final NodeHistoryRecorder historyRecorder;
    private final NodeLinkRepository nodeLinkRepository;
    private final TreeCollaborationRepository collaborationRepository;
    private final TreeStructureLock structureLock;
    private final EntityManager em;

    public TreeNodeLinkService(
        TeamAccessService teamAccessService,
        NodeHistoryRecorder historyRecorder,
        NodeLinkRepository nodeLinkRepository,
        TreeCollaborationRepository collaborationRepository,
        TreeStructureLock structureLock,
        EntityManager em
    ) {
        this.teamAccessService = teamAccessService;
        this.historyRecorder = historyRecorder;
        this.nodeLinkRepository = nodeLinkRepository;
        this.collaborationRepository = collaborationRepository;
        this.structureLock = structureLock;
        this.em = em;
    }

    /** Adds a link at the end of the node's list. History: LINK_ADDED "Link added" (prototype wording). */
    public TreeLinkDTO addLink(TreeNodeType type, Long nodeId, TreeLinkWriteDTO request) {
        Long teamId = teamAccessService.requireEditNode(type, nodeId);
        String name = validName(request == null ? null : request.name());
        String url = validUrl(request == null ? null : request.url());

        // max(sortOrder) + 1 under the team's structure lock, or concurrent adds share a sortOrder;
        // then make sure the node was not deleted by the previous lock holder.
        structureLock.lockTeam(teamId);
        structureLock.requireNode(type, nodeId, teamId);

        NodeLink link = new NodeLink().name(name).url(url).sortOrder(nextSortOrder(type, nodeId)).createdDate(Instant.now());
        switch (type) {
            case PRODUCT -> link.setProduct(em.getReference(Product.class, nodeId));
            case OUTCOME -> link.setOutcome(em.getReference(Outcome.class, nodeId));
            case OPPORTUNITY -> link.setOpportunity(em.getReference(Opportunity.class, nodeId));
            case SOLUTION -> link.setSolution(em.getReference(Solution.class, nodeId));
            case ASSUMPTION -> link.setAssumption(em.getReference(Assumption.class, nodeId));
            case EVIDENCE -> link.setEvidence(em.getReference(Evidence.class, nodeId));
        }
        link = nodeLinkRepository.save(link);
        historyRecorder.record(type, nodeId, HistoryEventType.LINK_ADDED, "Link added");
        return toDto(link);
    }

    /** Renames and/or re-points a link; absent fields are left unchanged. Writes no history. */
    public TreeLinkDTO updateLink(Long linkId, TreeLinkWriteDTO request) {
        teamAccessService.requireEditLink(linkId);
        NodeLink link = nodeLinkRepository.findById(linkId).orElseThrow(TeamAccessDeniedException::new);
        if (request != null && request.name() != null) {
            link.setName(validName(request.name()));
        }
        if (request != null && request.url() != null) {
            link.setUrl(validUrl(request.url()));
        }
        return toDto(nodeLinkRepository.save(link));
    }

    /** Removes a link. History: LINK_REMOVED "Link removed" (prototype wording). */
    public void deleteLink(Long linkId) {
        TreeNodeRef node = teamAccessService.requireEditLink(linkId);
        NodeLink link = nodeLinkRepository.findById(linkId).orElseThrow(TeamAccessDeniedException::new);
        nodeLinkRepository.delete(link);
        historyRecorder.record(node.type(), node.id(), HistoryEventType.LINK_REMOVED, "Link removed");
    }

    private int nextSortOrder(TreeNodeType type, Long nodeId) {
        Integer max = switch (type) {
            case PRODUCT -> collaborationRepository.maxLinkSortOrderOfProduct(nodeId);
            case OUTCOME -> collaborationRepository.maxLinkSortOrderOfOutcome(nodeId);
            case OPPORTUNITY -> collaborationRepository.maxLinkSortOrderOfOpportunity(nodeId);
            case SOLUTION -> collaborationRepository.maxLinkSortOrderOfSolution(nodeId);
            case ASSUMPTION -> collaborationRepository.maxLinkSortOrderOfAssumption(nodeId);
            case EVIDENCE -> collaborationRepository.maxLinkSortOrderOfEvidence(nodeId);
        };
        return max == null ? 0 : max + 1;
    }

    private static String validName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty() || name.length() > NAME_MAX) {
            throw new NodeWriteRuleException("Link name must be 1-" + NAME_MAX + " characters", ENTITY_NAME, "linknameinvalid");
        }
        return name;
    }

    private static String validUrl(String raw) {
        String url = raw == null ? "" : raw.trim();
        if (url.length() > URL_MAX || !URL_PATTERN.matcher(url).matches()) {
            throw new NodeWriteRuleException(
                "Link URL must start with http:// or https:// and be at most " + URL_MAX + " characters",
                ENTITY_NAME,
                "linkurlinvalid"
            );
        }
        return url;
    }

    private static TreeLinkDTO toDto(NodeLink link) {
        return new TreeLinkDTO(link.getId(), link.getName(), link.getUrl(), link.getSortOrder());
    }
}
