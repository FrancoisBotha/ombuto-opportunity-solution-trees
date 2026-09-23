package com.opportunity.tree.web.rest;

import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.service.LabelService;
import com.opportunity.tree.service.dto.tree.TreeNodeDTO;
import com.opportunity.tree.service.dto.tree.TreeNodeTagDTO;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LABEL-001: team-scoped label API — the panel's read/write path for labels on opportunities and
 * solutions. The generated admin-only {@code TagResource} stays as it is; every method here goes
 * through {@link LabelService} which enforces access via {@code TeamAccessService}.
 */
@RestController
@RequestMapping("/api/tree/labels")
public class LabelResource {

    private static final Logger LOG = LoggerFactory.getLogger(LabelResource.class);

    private final LabelService labelService;

    public LabelResource(LabelService labelService) {
        this.labelService = labelService;
    }

    /** Suggestions ordered by the caller's current team first, then their other teams. */
    @GetMapping("/suggestions/{teamId}")
    public List<TreeNodeTagDTO> suggestions(@PathVariable Long teamId) {
        LOG.debug("REST request to get label suggestions for team {}", teamId);
        return labelService.listSuggestions(teamId);
    }

    /** Inline create — POST {teamId, name}; returns existing tag when normalized name matches. */
    @PostMapping("/team/{teamId}")
    public ResponseEntity<TreeNodeTagDTO> createLabel(@PathVariable Long teamId, @RequestBody Map<String, Object> body) {
        String name = body == null ? null : (String) body.get("name");
        TreeNodeTagDTO created = labelService.createOrFindTag(teamId, name);
        return ResponseEntity.ok(created);
    }

    /**
     * Replace the tag set on an OPPORTUNITY or SOLUTION. Body: {@code {"tagIds":[...]}}.
     * Returns the updated tree node so the client can splice the response into its node list.
     */
    @PutMapping("/node/{type}/{id}")
    public TreeNodeDTO applyLabels(@PathVariable String type, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        TreeNodeType nodeType = TreeNodeType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        @SuppressWarnings("unchecked")
        List<Number> raw = body == null ? List.of() : (List<Number>) body.getOrDefault("tagIds", List.of());
        Set<Long> ids = raw.stream().map(Number::longValue).collect(java.util.stream.Collectors.toSet());
        return labelService.applyTags(nodeType, id, ids);
    }

    /** Preview: {@code {count: N}} — how many nodes carry this tag. */
    @GetMapping("/{tagId}/impact")
    public Map<String, Long> previewDelete(@PathVariable Long tagId) {
        return Map.of("count", labelService.previewDeletion(tagId));
    }

    /** Delete a tag and remove it from every node that carries it; body: {@code {removedFrom: N}}. */
    @DeleteMapping("/{tagId}")
    public Map<String, Long> deleteLabel(@PathVariable Long tagId) {
        long removed = labelService.deleteTag(tagId);
        return Map.of("removedFrom", removed);
    }
}
