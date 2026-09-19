package com.opportunity.tree.web.rest;

import com.opportunity.tree.service.TreeNodeHistoryService;
import com.opportunity.tree.service.TreeNodeRules;
import com.opportunity.tree.service.dto.tree.TreeHistoryEntryDTO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Per-node changelog read (FR-H1), newest first. Any member reads; PRODUCT has no history (400). */
@RestController
@RequestMapping("/api/tree")
public class TreeNodeHistoryResource {

    private static final Logger LOG = LoggerFactory.getLogger(TreeNodeHistoryResource.class);

    private final TreeNodeHistoryService treeNodeHistoryService;

    public TreeNodeHistoryResource(TreeNodeHistoryService treeNodeHistoryService) {
        this.treeNodeHistoryService = treeNodeHistoryService;
    }

    @GetMapping("/nodes/{type}/{id}/history")
    public List<TreeHistoryEntryDTO> getHistory(@PathVariable("type") String type, @PathVariable("id") Long id) {
        LOG.debug("REST request to get the history of {} {}", type, id);
        return treeNodeHistoryService.getHistory(TreeNodeRules.parseType(type, "type"), id);
    }
}
