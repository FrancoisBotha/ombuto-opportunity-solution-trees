package com.opportunity.tree.web.rest;

import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.service.TreeNodeRules;
import com.opportunity.tree.service.TreeNodeWriteService;
import com.opportunity.tree.service.dto.tree.CreateTreeNodeRequest;
import com.opportunity.tree.service.dto.tree.TreeNodeDTO;
import java.net.URI;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Team-authorised node writes for the OST tree builder:
 *
 * <ul>
 *   <li>{@code POST /api/tree/nodes} — create a node under a parent (201 + {@link TreeNodeDTO})</li>
 *   <li>{@code PATCH /api/tree/nodes/{type}/{id}} — merge-patch a node's fields (200 + {@link TreeNodeDTO})</li>
 *   <li>{@code DELETE /api/tree/nodes/{type}/{id}} — cascade delete a node and its subtree (204)</li>
 * </ul>
 *
 * {@code type} is a node type name, case-insensitive. Rules live in {@link TreeNodeWriteService}:
 * write-rule violations are 400 with an {@code error.<key>} message, viewers / non-members /
 * missing ids are 403. Moves are handled by {@link TreeNodeMoveResource}.
 */
@RestController
@RequestMapping("/api/tree/nodes")
public class TreeNodeResource {

    private static final Logger LOG = LoggerFactory.getLogger(TreeNodeResource.class);

    private final TreeNodeWriteService treeNodeWriteService;

    public TreeNodeResource(TreeNodeWriteService treeNodeWriteService) {
        this.treeNodeWriteService = treeNodeWriteService;
    }

    @PostMapping("")
    public ResponseEntity<TreeNodeDTO> createNode(@RequestBody CreateTreeNodeRequest request) {
        LOG.debug("REST request to create tree node : {}", request);
        TreeNodeDTO created = treeNodeWriteService.create(request);
        String path = "/api/tree/nodes/" + TreeNodeRules.label(created.getType()) + "/" + created.getId();
        return ResponseEntity.created(URI.create(path)).body(created);
    }

    @PatchMapping(value = "/{type}/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public TreeNodeDTO patchNode(@PathVariable("type") String type, @PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        LOG.debug("REST request to patch tree node {} {} : {}", type, id, body == null ? null : body.keySet());
        TreeNodeType nodeType = TreeNodeRules.parseType(type, "type");
        return treeNodeWriteService.patch(nodeType, id, body);
    }

    @DeleteMapping("/{type}/{id}")
    public ResponseEntity<Void> deleteNode(@PathVariable("type") String type, @PathVariable("id") Long id) {
        LOG.debug("REST request to cascade-delete tree node {} {}", type, id);
        TreeNodeType nodeType = TreeNodeRules.parseType(type, "type");
        treeNodeWriteService.delete(nodeType, id);
        return ResponseEntity.noContent().build();
    }
}
