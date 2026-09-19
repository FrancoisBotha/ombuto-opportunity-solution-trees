package com.opportunity.tree.service;

import com.opportunity.tree.domain.OpenQuestion;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.OpenQuestionRepository;
import com.opportunity.tree.repository.TreeCollaborationRepository;
import com.opportunity.tree.service.dto.tree.TreeQuestionDTO;
import com.opportunity.tree.service.dto.tree.TreeQuestionWriteDTO;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Open-question checklist on an opportunity (FR-Q1). Read = any team member
 * (questions come with the tree read); add / edit / tick / remove = OWNER or
 * EDITOR. Only adding a question is recorded in history.
 */
@Service
@Transactional
public class TreeOpenQuestionService {

    static final String ENTITY_NAME = "openQuestion";
    static final int TEXT_MAX = 500;

    private final TeamAccessService teamAccessService;
    private final NodeHistoryRecorder historyRecorder;
    private final OpenQuestionRepository openQuestionRepository;
    private final TreeCollaborationRepository collaborationRepository;
    private final TreeStructureLock structureLock;
    private final EntityManager em;

    public TreeOpenQuestionService(
        TeamAccessService teamAccessService,
        NodeHistoryRecorder historyRecorder,
        OpenQuestionRepository openQuestionRepository,
        TreeCollaborationRepository collaborationRepository,
        TreeStructureLock structureLock,
        EntityManager em
    ) {
        this.teamAccessService = teamAccessService;
        this.historyRecorder = historyRecorder;
        this.openQuestionRepository = openQuestionRepository;
        this.collaborationRepository = collaborationRepository;
        this.structureLock = structureLock;
        this.em = em;
    }

    /** Adds an open (not done) question at the end of the list. History: QUESTION_ADDED "Open question added". */
    public TreeQuestionDTO addQuestion(Long opportunityId, TreeQuestionWriteDTO request) {
        Long teamId = teamAccessService.requireEditNode(TreeNodeType.OPPORTUNITY, opportunityId);
        String text = validText(request == null ? null : request.text());
        // max(sortOrder) + 1 under the team's structure lock (see TreeStructureLock), then re-check.
        structureLock.lockTeam(teamId);
        structureLock.requireNode(TreeNodeType.OPPORTUNITY, opportunityId, teamId);
        Integer max = collaborationRepository.maxQuestionSortOrderOfOpportunity(opportunityId);
        OpenQuestion question = new OpenQuestion()
            .questionText(text)
            .done(false)
            .sortOrder(max == null ? 0 : max + 1)
            .createdDate(Instant.now())
            .opportunity(em.getReference(Opportunity.class, opportunityId));
        question = openQuestionRepository.save(question);
        historyRecorder.record(TreeNodeType.OPPORTUNITY, opportunityId, HistoryEventType.QUESTION_ADDED, "Open question added");
        return toDto(question);
    }

    /** Edits the text and/or ticks/unticks a question; absent fields are left unchanged. Writes no history. */
    public TreeQuestionDTO updateQuestion(Long questionId, TreeQuestionWriteDTO request) {
        teamAccessService.requireEditQuestion(questionId);
        OpenQuestion question = openQuestionRepository.findById(questionId).orElseThrow(TeamAccessDeniedException::new);
        if (request != null && request.text() != null) {
            question.setQuestionText(validText(request.text()));
        }
        if (request != null && request.done() != null) {
            question.setDone(request.done());
        }
        return toDto(openQuestionRepository.save(question));
    }

    /** Removes a question. Writes no history. */
    public void deleteQuestion(Long questionId) {
        teamAccessService.requireEditQuestion(questionId);
        openQuestionRepository.deleteById(questionId);
    }

    private static String validText(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty() || text.length() > TEXT_MAX) {
            throw new NodeWriteRuleException("Question text must be 1-" + TEXT_MAX + " characters", ENTITY_NAME, "questiontextinvalid");
        }
        return text;
    }

    private static TreeQuestionDTO toDto(OpenQuestion q) {
        return new TreeQuestionDTO(q.getId(), q.getQuestionText(), q.getDone(), q.getSortOrder());
    }
}
