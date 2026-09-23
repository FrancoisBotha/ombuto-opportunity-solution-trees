package com.opportunity.tree.web.rest;

import com.opportunity.tree.service.TreeMeetingTranscriptService;
import com.opportunity.tree.service.TreeNodeRules;
import com.opportunity.tree.service.dto.tree.TreeTranscriptDTO;
import com.opportunity.tree.service.dto.tree.TreeTranscriptMetaDTO;
import com.opportunity.tree.service.dto.tree.TreeTranscriptWriteDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

/**
 * Team-scoped meeting transcript endpoints (Epic 12 / MTRANS-002).
 *
 * <p>All routes live under {@code /api/tree} — the team-scoped surface, guarded by
 * {@link com.opportunity.tree.service.TeamAccessService} rather than the admin
 * {@code @PreAuthorize} that locks down the generated
 * {@link MeetingTranscriptResource}. List responses carry metadata only; the full body is only
 * returned by {@link #getTranscript(Long)} (NFR-022 / NFR-024).
 *
 * <p>Nothing logged from here carries the transcript body: parameters other than ids are omitted
 * on purpose so the pasted text never lands in an application log line.
 */
@RestController
@RequestMapping("/api/tree")
public class TreeMeetingTranscriptResource {

    private static final Logger LOG = LoggerFactory.getLogger(TreeMeetingTranscriptResource.class);

    private final TreeMeetingTranscriptService transcriptService;

    public TreeMeetingTranscriptResource(TreeMeetingTranscriptService transcriptService) {
        this.transcriptService = transcriptService;
    }

    @GetMapping("/nodes/{type}/{id}/transcripts")
    public ResponseEntity<java.util.List<TreeTranscriptMetaDTO>> listByNode(
        @PathVariable("type") String type,
        @PathVariable("id") Long id,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST list transcripts of {} {}", type, id);
        Page<TreeTranscriptMetaDTO> page = transcriptService.listByNode(TreeNodeRules.parseType(type, "type"), id, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/teams/{teamId}/transcripts")
    public ResponseEntity<java.util.List<TreeTranscriptMetaDTO>> listByTeam(
        @PathVariable("teamId") Long teamId,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST list transcripts of team {}", teamId);
        Page<TreeTranscriptMetaDTO> page = transcriptService.listByTeam(teamId, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/transcripts/{id}")
    public TreeTranscriptDTO getTranscript(@PathVariable("id") Long id) {
        LOG.debug("REST read transcript {}", id);
        return transcriptService.getTranscript(id);
    }

    @PostMapping("/transcripts")
    public ResponseEntity<TreeTranscriptDTO> create(@RequestBody(required = false) TreeTranscriptWriteDTO request) {
        LOG.debug("REST create transcript");
        return ResponseEntity.status(HttpStatus.CREATED).body(transcriptService.create(request));
    }

    @PatchMapping("/transcripts/{id}")
    public TreeTranscriptDTO update(@PathVariable("id") Long id, @RequestBody(required = false) TreeTranscriptWriteDTO request) {
        LOG.debug("REST edit transcript {}", id);
        return transcriptService.update(id, request);
    }

    @DeleteMapping("/transcripts/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        LOG.debug("REST delete transcript {}", id);
        transcriptService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
