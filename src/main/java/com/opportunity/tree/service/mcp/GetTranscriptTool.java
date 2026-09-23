package com.opportunity.tree.service.mcp;

import com.opportunity.tree.domain.MeetingTranscript;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.repository.MeetingTranscriptRepository;
import com.opportunity.tree.service.TeamAccessDeniedException;
import com.opportunity.tree.service.TeamAccessService;
import com.opportunity.tree.service.mcp.dto.TranscriptDetails;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only MCP tool that returns one meeting transcript with its full body.
 *
 * <p>This is the only MCP tool that ever returns the transcript body (NFR-022 / NFR-024). Access
 * is enforced through {@link TeamAccessService#requireReadTranscript} — non-members and cross-team
 * ids are refused with the same {@link TeamAccessDeniedException} shape whether the transcript
 * exists or not, so the response cannot be used to probe existence (NFR-002).
 */
@Service
@Transactional(readOnly = true)
public class GetTranscriptTool {

    private final TeamAccessService teamAccessService;
    private final MeetingTranscriptRepository transcriptRepository;

    public GetTranscriptTool(TeamAccessService teamAccessService, MeetingTranscriptRepository transcriptRepository) {
        this.teamAccessService = teamAccessService;
        this.transcriptRepository = transcriptRepository;
    }

    @Tool(
        name = "get_transcript",
        description = "Return a single meeting transcript by id, including its full body. Use this " +
            "after list_transcripts identifies a transcript worth reading. The caller must be a " +
            "member of the owning team; cross-team and unknown ids are refused with the same " +
            "access-denied error. Body is plain text with line breaks preserved."
    )
    public TranscriptDetails getTranscript(@ToolParam(description = "Numeric id of the transcript to fetch.") Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        teamAccessService.requireReadTranscript(id);
        MeetingTranscript t = transcriptRepository.findOneWithEagerRelationships(id).orElseThrow(TeamAccessDeniedException::new);
        return toDetails(t);
    }

    static TranscriptDetails toDetails(MeetingTranscript t) {
        User author = t.getAuthor();
        ListTranscriptsTool.NodeRef node = ListTranscriptsTool.nodeOf(t);
        return new TranscriptDetails(
            t.getId(),
            t.getTitle(),
            t.getMeetingDate(),
            t.getAttendees(),
            t.getBody(),
            t.getSource(),
            node == null ? null : node.type(),
            node == null ? null : node.id(),
            author == null ? null : author.getLogin(),
            t.getCreatedDate(),
            t.getEditedDate()
        );
    }
}
