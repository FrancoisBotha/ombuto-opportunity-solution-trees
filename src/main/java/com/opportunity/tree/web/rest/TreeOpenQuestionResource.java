package com.opportunity.tree.web.rest;

import com.opportunity.tree.service.TreeOpenQuestionService;
import com.opportunity.tree.service.dto.tree.TreeQuestionDTO;
import com.opportunity.tree.service.dto.tree.TreeQuestionWriteDTO;
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
 * Open questions on an opportunity (FR-Q1). OWNER / EDITOR may write; viewers,
 * non-members and unknown ids (including ids that are not opportunities) get 403.
 */
@RestController
@RequestMapping("/api/tree")
public class TreeOpenQuestionResource {

    private static final Logger LOG = LoggerFactory.getLogger(TreeOpenQuestionResource.class);

    private final TreeOpenQuestionService treeOpenQuestionService;

    public TreeOpenQuestionResource(TreeOpenQuestionService treeOpenQuestionService) {
        this.treeOpenQuestionService = treeOpenQuestionService;
    }

    @PostMapping("/opportunities/{id}/questions")
    public ResponseEntity<TreeQuestionDTO> addQuestion(
        @PathVariable("id") Long id,
        @RequestBody(required = false) TreeQuestionWriteDTO request
    ) {
        LOG.debug("REST request to add an open question to opportunity {}", id);
        return ResponseEntity.status(HttpStatus.CREATED).body(treeOpenQuestionService.addQuestion(id, request));
    }

    @PatchMapping("/questions/{id}")
    public TreeQuestionDTO updateQuestion(@PathVariable("id") Long id, @RequestBody(required = false) TreeQuestionWriteDTO request) {
        LOG.debug("REST request to update open question {}", id);
        return treeOpenQuestionService.updateQuestion(id, request);
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete open question {}", id);
        treeOpenQuestionService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }
}
