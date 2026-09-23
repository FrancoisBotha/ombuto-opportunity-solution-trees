package com.opportunity.tree.service;

import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Tag;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.TagRepository;
import com.opportunity.tree.service.broadcast.TreeChangePublisher;
import com.opportunity.tree.service.broadcast.TreeChangeType;
import com.opportunity.tree.service.dto.tree.TreeNodeDTO;
import com.opportunity.tree.service.dto.tree.TreeNodeTagDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * LABEL-001: team-scoped label API. Sits above the generated admin-only {@code TagResource} and
 * routes every read/write through {@link TeamAccessService}. Handles inline "create if missing",
 * suggestion ordering (current team first, then the caller's other teams), and cross-team
 * rejection when applying labels to a node.
 */
@Service
@Transactional
public class LabelService {

    private static final Logger LOG = LoggerFactory.getLogger(LabelService.class);
    private static final int MAX_NAME_LENGTH = 50;

    private final TeamAccessService teamAccessService;
    private final TagRepository tagRepository;
    private final NodeHistoryRecorder historyRecorder;
    private final TreeNodeDtoAssembler dtoAssembler;
    private final TreeChangePublisher changePublisher;
    private final TreeStructureLock structureLock;

    @PersistenceContext
    private EntityManager em;

    public LabelService(
        TeamAccessService teamAccessService,
        TagRepository tagRepository,
        NodeHistoryRecorder historyRecorder,
        TreeNodeDtoAssembler dtoAssembler,
        TreeChangePublisher changePublisher,
        TreeStructureLock structureLock
    ) {
        this.teamAccessService = teamAccessService;
        this.tagRepository = tagRepository;
        this.historyRecorder = historyRecorder;
        this.dtoAssembler = dtoAssembler;
        this.changePublisher = changePublisher;
        this.structureLock = structureLock;
    }

    /**
     * All tags accessible to the caller, ordered so the given team's tags come first, then the
     * caller's other member teams. Used to build the panel's suggestion list.
     */
    @Transactional(readOnly = true)
    public List<TreeNodeTagDTO> listSuggestions(Long teamId) {
        teamAccessService.requireReadTeam(teamId);
        List<TreeNodeTagDTO> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Tag t : tagRepository.findAllByTeamId(teamId)) {
            if (seen.add(t.getId())) {
                result.add(new TreeNodeTagDTO(t.getId(), t.getName()));
            }
        }
        Set<Long> otherTeams = new HashSet<>(teamAccessService.getCurrentUserTeamIds());
        otherTeams.remove(teamId);
        if (!otherTeams.isEmpty()) {
            // A tag identical by name (different team row) still shows once — path-of-least
            // resistance for identical text (epic decision 2026-09-21).
            Set<String> seenNormalized = result
                .stream()
                .map(t -> Tag.normalize(t.name()))
                .collect(Collectors.toCollection(HashSet::new));
            for (Tag t : tagRepository.findAllByTeamIdIn(otherTeams)) {
                if (seenNormalized.add(Tag.normalize(t.getName()))) {
                    result.add(new TreeNodeTagDTO(t.getId(), t.getName()));
                }
            }
        }
        return result;
    }

    /**
     * Creates a tag in {@code teamId} if one with the same normalized name does not already exist
     * there. Returns the (possibly pre-existing) tag as a lightweight DTO.
     */
    public TreeNodeTagDTO createOrFindTag(Long teamId, String name) {
        teamAccessService.requireEditTeam(teamId);
        String trimmed = validName(name);
        String normalized = Tag.normalize(trimmed);
        return tagRepository
            .findByTeamIdAndNormalizedName(teamId, normalized)
            .map(existing -> new TreeNodeTagDTO(existing.getId(), existing.getName()))
            .orElseGet(() -> {
                Team team = em.find(Team.class, teamId);
                if (team == null) {
                    throw new TeamAccessDeniedException();
                }
                Tag tag = new Tag();
                tag.setTeam(team);
                tag.setName(trimmed);
                tag.setNormalizedName(normalized);
                em.persist(tag);
                em.flush();
                return new TreeNodeTagDTO(tag.getId(), tag.getName());
            });
    }

    /**
     * Applies a set of tag ids to an OPPORTUNITY or SOLUTION node. Tag ids must belong to the
     * same team as the node — otherwise the write is rejected server-side with a 400 (not merely
     * impossible in the UI). History and change publication are handled by the caller.
     */
    public TreeNodeDTO applyTags(TreeNodeType type, Long id, Collection<Long> tagIds) {
        if (type != TreeNodeType.OPPORTUNITY && type != TreeNodeType.SOLUTION) {
            throw new NodeWriteRuleException(
                "Labels only apply to opportunities and solutions",
                TreeNodeRules.ENTITY_NAME,
                "fieldnotapplicable"
            );
        }
        Long teamId = teamAccessService.requireEditNode(type, id);
        structureLock.lockTeam(teamId);
        structureLock.requireNode(type, id, teamId);

        Set<Long> requested = tagIds == null ? Set.of() : new HashSet<>(tagIds);
        List<Tag> tags = requested.isEmpty() ? List.of() : tagRepository.findAllById(requested);
        if (tags.size() != requested.size()) {
            throw new NodeWriteRuleException("Unknown tag", TreeNodeRules.ENTITY_NAME, "unknowntag");
        }
        for (Tag t : tags) {
            if (t.getTeam() == null || !Objects.equals(t.getTeam().getId(), teamId)) {
                throw new NodeWriteRuleException(
                    "Tag " + t.getId() + " belongs to a different team",
                    TreeNodeRules.ENTITY_NAME,
                    "crossteamtag"
                );
            }
        }

        Set<Tag> next = new HashSet<>(tags);
        Set<Long> nextIds = next.stream().map(Tag::getId).collect(Collectors.toSet());
        List<String> beforeNames;
        List<String> afterNames;
        if (type == TreeNodeType.OPPORTUNITY) {
            Opportunity o = em.find(Opportunity.class, id);
            beforeNames = o.getTags().stream().map(Tag::getName).sorted().toList();
            Set<Long> beforeIds = o.getTags().stream().map(Tag::getId).collect(Collectors.toSet());
            if (!beforeIds.equals(nextIds)) {
                o.setTags(next);
            }
            afterNames = next.stream().map(Tag::getName).sorted().toList();
        } else {
            Solution s = em.find(Solution.class, id);
            beforeNames = s.getTags().stream().map(Tag::getName).sorted().toList();
            Set<Long> beforeIds = s.getTags().stream().map(Tag::getId).collect(Collectors.toSet());
            if (!beforeIds.equals(nextIds)) {
                s.setTags(next);
            }
            afterNames = next.stream().map(Tag::getName).sorted().toList();
        }
        em.flush();

        if (!beforeNames.equals(afterNames)) {
            String summary = afterNames.isEmpty() ? "Labels cleared" : "Labels: " + String.join(", ", afterNames);
            historyRecorder.record(type, id, HistoryEventType.TAGS_CHANGED, summary);
        }
        TreeNodeDTO dto = dtoAssembler.toDto(type, id);
        changePublisher.publish(TreeChangeType.NODE_UPDATED, teamId, dto);
        return dto;
    }

    /** Preview count of nodes that carry this tag; caller must have read access to the team. */
    @Transactional(readOnly = true)
    public long previewDeletion(Long tagId) {
        Tag tag = tagRepository.findById(tagId).orElseThrow(TeamAccessDeniedException::new);
        teamAccessService.requireEditTeam(tag.getTeam().getId());
        return tagRepository.countAssociations(tagId);
    }

    /**
     * Deletes the tag; JPA cascades remove entries from both join tables because the tag owns the
     * inverse side of the many-to-many. The return value is the number of node associations that
     * were removed, so the panel can render "removed from N nodes".
     */
    public long deleteTag(Long tagId) {
        Tag tag = tagRepository.findById(tagId).orElseThrow(TeamAccessDeniedException::new);
        Long teamId = tag.getTeam().getId();
        teamAccessService.requireEditTeam(teamId);
        long affected = tagRepository.countAssociations(tagId);
        // Break associations on the owning side, then delete the tag.
        for (Opportunity o : new ArrayList<>(tag.getOpportunities())) {
            o.getTags().remove(tag);
        }
        for (Solution s : new ArrayList<>(tag.getSolutions())) {
            s.getTags().remove(tag);
        }
        tag.getOpportunities().clear();
        tag.getSolutions().clear();
        tagRepository.delete(tag);
        em.flush();
        LOG.debug("Deleted tag {} (removed from {} nodes)", tagId, affected);
        return affected;
    }

    private static String validName(String raw) {
        if (raw == null) {
            throw new NodeWriteRuleException("name is required", TreeNodeRules.ENTITY_NAME, "invalidtagname");
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH) {
            throw new NodeWriteRuleException(
                "name must be 1-" + MAX_NAME_LENGTH + " characters",
                TreeNodeRules.ENTITY_NAME,
                "invalidtagname"
            );
        }
        return trimmed;
    }

    /**
     * LABEL-001: a stable map view of {@link #listSuggestions(Long)} keyed by team id. Useful for
     * tests that want to assert ordering; production callers should just use the list.
     */
    Map<Long, List<String>> suggestionsGroupedByTeamId(Long teamId) {
        teamAccessService.requireReadTeam(teamId);
        Map<Long, List<String>> map = new LinkedHashMap<>();
        map.put(teamId, tagRepository.findAllByTeamId(teamId).stream().map(Tag::getName).toList());
        return map;
    }
}
