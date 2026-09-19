package com.opportunity.tree.service;

import com.opportunity.tree.domain.OpenQuestion;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.OpenQuestionRepository;
import com.opportunity.tree.repository.TreeCollaborationRepository;
import com.opportunity.tree.service.broadcast.QuestionChangedPayload;
import com.opportunity.tree.service.broadcast.QuestionRemovedPayload;
import com.opportunity.tree.service.broadcast.TreeChangePublisher;
import com.opportunity.tree.service.broadcast.TreeChangeType;
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
 *
 * <p>Every write publishes a {@code QUESTION_*} tree change event after commit through
 * {@link TreeChangePublisher}, holding the team's {@link TreeStructureLock} so that per-team
 * {@code seq} ordering is preserved. Nothing is published for a rejected or rolled-back write.
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
    private final TreeChangePublisher changePublisher;
    private final EntityManager em;

    public TreeOpenQuestionService(
        TeamAccessService teamAccessService,
        NodeHistoryRecorder historyRecorder,
        OpenQuestionRepository openQuestionRepository,
        TreeCollaborationRepository collaborationRepository,
        TreeStructureLock structureLock,
        TreeChangePublisher changePublisher,
        EntityManager em
    ) {
        this.teamAccessService = teamAccessService;
        this.historyRecorder = historyRecorder;
        this.openQuestionRepository = openQuestionRepository;
        this.collaborationRepository = collaborationRepository;
        this.structureLock = structureLock;
        this.changePublisher = changePublisher;
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
        TreeQuestionDTO dto = toDto(question);
        changePublisher.publish(
            TreeChangeType.QUESTION_ADDED,
            teamId,
            new QuestionChangedPayload(TreeNodeRef.key(TreeNodeType.OPPORTUNITY, opportunityId), dto)
        );
        return dto;
    }

    /** Edits the text and/or ticks/unticks a question; absent fields are left unchanged. Writes no history. */
    public TreeQuestionDTO updateQuestion(Long questionId, TreeQuestionWriteDTO request) {
        TreeNodeRef node = teamAccessService.requireEditQuestion(questionId);
        Long teamId = teamAccessService.teamIdForNode(node.type(), node.id()).orElseThrow(TeamAccessDeniedException::new);
        structureLock.lockTeam(teamId);
        OpenQuestion question = openQuestionRepository.findById(questionId).orElseThrow(TeamAccessDeniedException::new);
        if (request != null && request.text() != null) {
            question.setQuestionText(validText(request.text()));
        }
        if (request != null && request.done() != null) {
            question.setDone(request.done());
        }
        TreeQuestionDTO dto = toDto(openQuestionRepository.save(question));
        changePublisher.publish(TreeChangeType.QUESTION_UPDATED, teamId, new QuestionChangedPayload(node.key(), dto));
        return dto;
    }

    /** Removes a question. Writes no history. */
    public void deleteQuestion(Long questionId) {
        TreeNodeRef node = teamAccessService.requireEditQuestion(questionId);
        Long teamId = teamAccessService.teamIdForNode(node.type(), node.id()).orElseThrow(TeamAccessDeniedException::new);
        structureLock.lockTeam(teamId);
        openQuestionRepository.deleteById(questionId);
        changePublisher.publish(TreeChangeType.QUESTION_REMOVED, teamId, new QuestionRemovedPayload(node.key(), questionId));
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
