package com.opportunity.tree.web.rest;

import com.opportunity.tree.service.TreeNodeMoveService;
import com.opportunity.tree.service.dto.tree.MoveTreeNodeRequest;
import com.opportunity.tree.service.dto.tree.MoveTreeNodeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/tree/nodes/move} — re-parent and/or reorder a tree node. Returns the moved
 * node plus the renumbered sortOrder of every affected sibling. Rules and authorisation live in
 * {@link TreeNodeMoveService}.
 */
@RestController
@RequestMapping("/api/tree/nodes")
public class TreeNodeMoveResource {

    private static final Logger LOG = LoggerFactory.getLogger(TreeNodeMoveResource.class);

    private final TreeNodeMoveService treeNodeMoveService;

    public TreeNodeMoveResource(TreeNodeMoveService treeNodeMoveService) {
        this.treeNodeMoveService = treeNodeMoveService;
    }

    @PostMapping("/move")
    public MoveTreeNodeResponse move(@RequestBody MoveTreeNodeRequest request) {
        LOG.debug("REST request to move tree node : {}", request);
        return treeNodeMoveService.move(request);
    }
}
