package com.opportunity.tree.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.OpportunityRepository;
import com.opportunity.tree.service.NodeHistoryRecorder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Transactional rollback of {@code POST /api/tree/nodes/move} with a forced mid-move failure
 * (ported from REARR-001). The class runs without a test-managed transaction so the service's
 * own transaction commits or rolls back for real. A {@link MockitoSpyBean} on
 * {@link NodeHistoryRecorder} makes the MOVED history write — the last step of a move, after the
 * re-parent, the nested outcome propagation and both sibling renumbers — throw. Every mutated
 * field is then read back in a fresh transaction: parent, outcome references and every sortOrder
 * must be unchanged, and no history row may survive.
 */
@IntegrationTest
@AutoConfigureMockMvc
class TreeNodeMoveRollbackIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformTransactionManager txMgr;

    @Autowired
    private OpportunityRepository opportunityRepository;

    @MockitoSpyBean
    private NodeHistoryRecorder historyRecorder;

    @PersistenceContext
    private EntityManager em;

    private TransactionTemplate tt;

    private String ownerLogin;
    private Long teamId;
    private Long productId;
    private Long outcomeAId;
    private Long outcomeBId;
    private Long opp1Id;
    private Long opp2Id;
    private Long opp3Id;
    private Long sib1Id;
    private Long opp4Id;
    private Long sol1Id;

    @BeforeEach
    void seed() {
        tt = new TransactionTemplate(txMgr);
        Mockito.reset(historyRecorder);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        ownerLogin = "rollback-owner-" + suffix;

        tt.executeWithoutResult(status -> {
            OstTreeTestData data = new OstTreeTestData(em);
            Team team = data.team("Rollback Team " + suffix);
            User owner = data.user(ownerLogin);
            data.member(team, owner, TeamRole.OWNER);
            Product product = data.product(team, "rb-prod-" + suffix, 0);
            Outcome outA = data.outcome(product, "out-A-" + suffix, 1);
            Outcome outB = data.outcome(product, "out-B-" + suffix, 2);
            Opportunity opp1 = data.opportunity(outA, null, "opp1-" + suffix, 1);
            Opportunity opp2 = data.opportunity(outA, opp1, "opp2-" + suffix, 1);
            Opportunity opp3 = data.opportunity(outA, opp2, "opp3-" + suffix, 1);
            Opportunity sib1 = data.opportunity(outA, null, "sib1-" + suffix, 2);
            Opportunity opp4 = data.opportunity(outB, null, "opp4-" + suffix, 1);
            Solution sol1 = data.solution(opp3, "sol1-" + suffix, 1);
            em.flush();

            teamId = team.getId();
            productId = product.getId();
            outcomeAId = outA.getId();
            outcomeBId = outB.getId();
            opp1Id = opp1.getId();
            opp2Id = opp2.getId();
            opp3Id = opp3.getId();
            sib1Id = sib1.getId();
            opp4Id = opp4.getId();
            sol1Id = sol1.getId();
        });
    }

    @AfterEach
    void cleanup() {
        Mockito.reset(historyRecorder);
        tt.executeWithoutResult(status -> {
            em
                .createQuery("delete from NodeHistory h where h.nodeType = :t and h.nodeId in :ids")
                .setParameter("t", TreeNodeType.OPPORTUNITY)
                .setParameter("ids", List.of(opp1Id, opp2Id, opp3Id, sib1Id, opp4Id))
                .executeUpdate();
            em.createQuery("delete from Solution s where s.id = :id").setParameter("id", sol1Id).executeUpdate();
            em
                .createNativeQuery("update opportunity set parent_id = null where id in (:ids)")
                .setParameter("ids", List.of(opp1Id, opp2Id, opp3Id, sib1Id, opp4Id))
                .executeUpdate();
            em
                .createQuery("delete from Opportunity o where o.id in :ids")
                .setParameter("ids", List.of(opp1Id, opp2Id, opp3Id, sib1Id, opp4Id))
                .executeUpdate();
            em.createQuery("delete from Outcome o where o.id in :ids").setParameter("ids", List.of(outcomeAId, outcomeBId)).executeUpdate();
            em.createQuery("delete from Product p where p.id = :id").setParameter("id", productId).executeUpdate();
            em.createQuery("delete from TeamMember tm where tm.team.id = :id").setParameter("id", teamId).executeUpdate();
            em.createQuery("delete from Team t where t.id = :id").setParameter("id", teamId).executeUpdate();
            em.createQuery("delete from User u where u.login = :l").setParameter("l", ownerLogin).executeUpdate();
        });
    }

    @Test
    void failureAtTheLastStepOfAMoveRollsBackEveryChange() throws Exception {
        int[] snapshot = tt.execute(status ->
            new int[] {
                opportunityRepository.findById(opp1Id).orElseThrow().getSortOrder(),
                opportunityRepository.findById(opp4Id).orElseThrow().getSortOrder(),
                opportunityRepository.findById(sib1Id).orElseThrow().getSortOrder(),
                opportunityRepository.findById(opp2Id).orElseThrow().getSortOrder(),
                opportunityRepository.findById(opp3Id).orElseThrow().getSortOrder(),
            }
        );

        doThrow(new IllegalStateException("forced mid-move failure"))
            .when(historyRecorder)
            .record(any(), any(), eq(HistoryEventType.MOVED), any());

        Map<String, Object> body = new HashMap<>();
        body.put("nodeType", "OPPORTUNITY");
        body.put("nodeId", opp1Id);
        body.put("parentType", "OPPORTUNITY");
        body.put("parentId", opp4Id);
        body.put("position", 0);

        mvc
            .perform(
                post("/api/tree/nodes/move")
                    .with(user(ownerLogin))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(body))
            )
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isGreaterThanOrEqualTo(500));
        verify(historyRecorder).record(eq(TreeNodeType.OPPORTUNITY), eq(opp1Id), eq(HistoryEventType.MOVED), any());

        tt.executeWithoutResult(status -> {
            Opportunity o1 = opportunityRepository.findById(opp1Id).orElseThrow();
            Opportunity o2 = opportunityRepository.findById(opp2Id).orElseThrow();
            Opportunity o3 = opportunityRepository.findById(opp3Id).orElseThrow();
            Opportunity oSib1 = opportunityRepository.findById(sib1Id).orElseThrow();
            Opportunity o4 = opportunityRepository.findById(opp4Id).orElseThrow();

            assertThat(o1.getParent()).as("opp1 parent must be unchanged").isNull();
            assertThat(o1.getOutcome().getId()).as("opp1 outcome must be unchanged").isEqualTo(outcomeAId);
            assertThat(o2.getOutcome().getId()).as("opp2 outcome must be unchanged").isEqualTo(outcomeAId);
            assertThat(o3.getOutcome().getId()).as("opp3 outcome must be unchanged").isEqualTo(outcomeAId);
            assertThat(o1.getSortOrder()).as("opp1 sortOrder rollback").isEqualTo(snapshot[0]);
            assertThat(o4.getSortOrder()).as("opp4 sortOrder rollback").isEqualTo(snapshot[1]);
            assertThat(oSib1.getSortOrder()).as("sib1 sortOrder rollback").isEqualTo(snapshot[2]);
            assertThat(o2.getSortOrder()).as("opp2 sortOrder rollback").isEqualTo(snapshot[3]);
            assertThat(o3.getSortOrder()).as("opp3 sortOrder rollback").isEqualTo(snapshot[4]);
            Long history = em
                .createQuery("select count(h) from NodeHistory h where h.nodeType = :t and h.nodeId = :id", Long.class)
                .setParameter("t", TreeNodeType.OPPORTUNITY)
                .setParameter("id", opp1Id)
                .getSingleResult();
            assertThat(history).as("no MOVED history survives the rollback").isZero();
        });
    }
}
