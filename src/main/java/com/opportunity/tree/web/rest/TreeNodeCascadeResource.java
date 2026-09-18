package com.opportunity.tree.web.rest;

import com.opportunity.tree.service.TreeNodeCascadeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Team-authorised cascade delete endpoints for tree nodes (TREE-003 / FR-016).
 *
 * <p>The generated {@code OutcomeResource}, {@code OpportunityResource} and
 * {@code SolutionResource} restrict every mutation to {@code ADMIN}, which is
 * appropriate for the raw CRUD API but useless for team members editing their
 * tree. These endpoints route through {@link TreeNodeCascadeService} which
 * enforces the team role rules through {@code TeamAccessService}: owners and
 * editors succeed, viewers and non-members receive HTTP 403 and nothing is
 * deleted.
 */
@RestController
@RequestMapping("/api/tree")
public class TreeNodeCascadeResource {

    private static final Logger LOG = LoggerFactory.getLogger(TreeNodeCascadeResource.class);

    private final TreeNodeCascadeService treeNodeCascadeService;

    public TreeNodeCascadeResource(TreeNodeCascadeService treeNodeCascadeService) {
        this.treeNodeCascadeService = treeNodeCascadeService;
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable("id") Long id) {
        LOG.debug("REST request to cascade-delete Product {}", id);
        treeNodeCascadeService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/outcomes/{id}")
    public ResponseEntity<Void> deleteOutcome(@PathVariable("id") Long id) {
        LOG.debug("REST request to cascade-delete Outcome {}", id);
        treeNodeCascadeService.deleteOutcome(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/opportunities/{id}")
    public ResponseEntity<Void> deleteOpportunity(@PathVariable("id") Long id) {
        LOG.debug("REST request to cascade-delete Opportunity {}", id);
        treeNodeCascadeService.deleteOpportunity(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/solutions/{id}")
    public ResponseEntity<Void> deleteSolution(@PathVariable("id") Long id) {
        LOG.debug("REST request to cascade-delete Solution {}", id);
        treeNodeCascadeService.deleteSolution(id);
        return ResponseEntity.noContent().build();
    }
}
