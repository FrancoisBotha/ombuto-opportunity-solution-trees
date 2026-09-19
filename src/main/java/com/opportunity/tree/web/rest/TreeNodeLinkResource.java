package com.opportunity.tree.web.rest;

import com.opportunity.tree.service.TreeNodeLinkService;
import com.opportunity.tree.service.dto.tree.TreeLinkDTO;
import com.opportunity.tree.service.dto.tree.TreeLinkWriteDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Links on any tree node (FR-D4). Team-authorised through {@code TeamAccessService}:
 * OWNER / EDITOR may write; viewers, non-members and unknown ids get 403.
 */
@RestController
@RequestMapping("/api/tree")
public class TreeNodeLinkResource {

    private static final Logger LOG = LoggerFactory.getLogger(TreeNodeLinkResource.class);

    private final TreeNodeLinkService treeNodeLinkService;

    public TreeNodeLinkResource(TreeNodeLinkService treeNodeLinkService) {
        this.treeNodeLinkService = treeNodeLinkService;
    }

    @PostMapping("/nodes/{type}/{id}/links")
    public ResponseEntity<TreeLinkDTO> addLink(
        @PathVariable("type") String type,
        @PathVariable("id") Long id,
        @RequestBody(required = false) TreeLinkWriteDTO request
    ) {
        LOG.debug("REST request to add a link to {} {}", type, id);
        return ResponseEntity.status(HttpStatus.CREATED).body(treeNodeLinkService.addLink(TreeNodeTypePath.parse(type), id, request));
    }

    @PatchMapping("/links/{id}")
    public TreeLinkDTO updateLink(@PathVariable("id") Long id, @RequestBody(required = false) TreeLinkWriteDTO request) {
        LOG.debug("REST request to update link {}", id);
        return treeNodeLinkService.updateLink(id, request);
    }

    @DeleteMapping("/links/{id}")
    public ResponseEntity<Void> deleteLink(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete link {}", id);
        treeNodeLinkService.deleteLink(id);
        return ResponseEntity.noContent().build();
    }
}
