package com.opportunity.tree.web.rest;

import com.opportunity.tree.service.TreeCommentService;
import com.opportunity.tree.service.TreeNodeRules;
import com.opportunity.tree.service.dto.tree.TreeCommentDTO;
import com.opportunity.tree.service.dto.tree.TreeCommentWriteDTO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Per-node chat (FR-M1..M3). Any member reads; OWNER / EDITOR post; only the
 * author (while still OWNER / EDITOR) edits or deletes. PRODUCT has no chat (400).
 */
@RestController
@RequestMapping("/api/tree")
public class TreeCommentResource {

    private static final Logger LOG = LoggerFactory.getLogger(TreeCommentResource.class);

    private final TreeCommentService treeCommentService;

    public TreeCommentResource(TreeCommentService treeCommentService) {
        this.treeCommentService = treeCommentService;
    }

    @GetMapping("/nodes/{type}/{id}/comments")
    public List<TreeCommentDTO> getComments(@PathVariable("type") String type, @PathVariable("id") Long id) {
        LOG.debug("REST request to get the chat of {} {}", type, id);
        return treeCommentService.getComments(TreeNodeRules.parseType(type, "type"), id);
    }

    @PostMapping("/nodes/{type}/{id}/comments")
    public ResponseEntity<TreeCommentDTO> addComment(
        @PathVariable("type") String type,
        @PathVariable("id") Long id,
        @RequestBody(required = false) TreeCommentWriteDTO request
    ) {
        LOG.debug("REST request to post a comment on {} {}", type, id);
        return ResponseEntity.status(HttpStatus.CREATED).body(treeCommentService.addComment(TreeNodeRules.parseType(type, "type"), id, request));
    }

    @PatchMapping("/comments/{id}")
    public TreeCommentDTO updateComment(@PathVariable("id") Long id, @RequestBody(required = false) TreeCommentWriteDTO request) {
        LOG.debug("REST request to edit comment {}", id);
        return treeCommentService.updateComment(id, request);
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete comment {}", id);
        treeCommentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
