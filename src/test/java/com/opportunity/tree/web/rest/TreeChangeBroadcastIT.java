package com.opportunity.tree.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.service.TreeNodeWriteService;
import com.opportunity.tree.service.broadcast.NodeDeletedPayload;
import com.opportunity.tree.service.broadcast.TreeChangeEvent;
import com.opportunity.tree.service.broadcast.TreeChangePublisher;
import com.opportunity.tree.service.broadcast.TreeChangeType;
import com.opportunity.tree.service.dto.tree.CreateTreeNodeRequest;
import com.opportunity.tree.service.dto.tree.MoveTreeNodeRequest;
import com.opportunity.tree.service.dto.tree.MoveTreeNodeResponse;
import com.opportunity.tree.service.dto.tree.TreeNodeDTO;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Integration tests for the tree change-event model and after-commit publisher (RTC-001).
 *
 * <p>Verifies that node writes broadcast a {@link TreeChangeEvent} to
 * {@code /topic/teams/{teamId}/tree} strictly after commit, that rejected or rolled-back writes
 * publish nothing, that {@code seq} is monotonic per team, and that a {@code NODE_DELETED} event
 * carries every cascaded descendant key.
 */
@IntegrationTest
@AutoConfigureMockMvc
class TreeChangeBroadcastIT {

    private static final String OWNER = "rtc-owner";
    private static final String EDITOR = "rtc-editor";
    private static final String VIEWER = "rtc-viewer";

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private EntityManager em;

    @Autowired
    private TreeNodeWriteService writeService;

    @Autowired
    private TreeChangePublisher publisher;

    @Autowired
    private PlatformTransactionManager txMgr;

    private OstTreeTestData data;
    private Team team;
    private Team otherTeam;
    private Product product;
    private Outcome outcome;
    private Opportunity opportunity;
    private Solution solution;
    private Assumption assumption;
    private Evidence evidence;
    private Product otherProduct;
    private Opportunity otherOpportunity;

    @BeforeEach
    void setUp() {
        new TransactionTemplate(txMgr).execute(status -> {
            createFixture();
            return null;
        });
    }

    /**
     * This IT is intentionally non-transactional (the AFTER_COMMIT broadcast only fires on a real
     * commit), so everything {@link #setUp()} seeds is committed. Without this teardown those rows
     * survive into later tests in the same JVM — the leftover {@code team_member} rows made
     * {@code UserResourceIT.testUserEquals} fail with {@code fk_team_member__user_id}. Remove
     * exactly what this class created, in FK-safe order.
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        List<Long> teamIds = Stream.of(team, otherTeam).filter(Objects::nonNull).map(Team::getId).filter(Objects::nonNull).toList();
        OstTreeTestCleanup.removeTeamsAndUsers(txMgr, em, teamIds, List.of(OWNER, EDITOR, VIEWER));
    }

    private void createFixture() {
        data = new OstTreeTestData(em);
        team = data.team("RTC Team");
        otherTeam = data.team("RTC Other Team");
        data.member(team, data.user(OWNER), TeamRole.OWNER);
        data.member(team, data.user(EDITOR), TeamRole.EDITOR);
        data.member(team, data.user(VIEWER), TeamRole.VIEWER);
        data.member(otherTeam, data.user(OWNER), TeamRole.OWNER);
        product = data.product(team, "RTC product", 0);
        outcome = data.outcome(product, "RTC outcome", 0);
        opportunity = data.opportunity(outcome, null, "RTC opportunity", 0);
        solution = data.solution(opportunity, "RTC solution", 0);
        assumption = data.assumption(solution, "RTC assumption", 0);
        evidence = data.evidence(null, assumption, "RTC evidence", 0);
        otherProduct = data.product(otherTeam, "RTC other product", 0);
        Outcome otherOutcome = data.outcome(otherProduct, "RTC other outcome", 0);
        otherOpportunity = data.opportunity(otherOutcome, null, "RTC other opportunity", 0);
        em.flush();
    }

    // AC #1, #2, #3, #6 — create publishes NODE_CREATED with TreeNodeDTO after commit, requestId echoed.
    @Test
    void createPublishesNodeCreatedAfterCommit() throws Exception {
        CreateTreeNodeRequest body = new CreateTreeNodeRequest("outcome", "product", product.getId(), "Broadcast outcome");
        mvc
            .perform(
                post("/api/tree/nodes")
                    .with(user(EDITOR))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(TreeChangePublisher.REQUEST_ID_HEADER, "req-create-1")
                    .content(om.writeValueAsString(body))
            )
            .andExpect(status().isCreated());

        TreeChangeEvent event = capturedEvent();
        assertThat(event.type()).isEqualTo(TreeChangeType.NODE_CREATED);
        assertThat(event.teamId()).isEqualTo(team.getId());
        assertThat(event.actingUserLogin()).isEqualTo(EDITOR);
        assertThat(event.at()).isNotNull();
        assertThat(event.seq()).isPositive();
        assertThat(event.epoch()).isEqualTo(publisher.epoch());
        assertThat(event.requestId()).isEqualTo("req-create-1");
        assertThat(event.payload()).isInstanceOf(TreeNodeDTO.class);
        TreeNodeDTO dto = (TreeNodeDTO) event.payload();
        assertThat(dto.getType()).isEqualTo(TreeNodeType.OUTCOME);
        assertThat(dto.getTitle()).isEqualTo("Broadcast outcome");
    }

    // AC #6 — a missing request id is tolerated (requestId == null).
    @Test
    void patchWithoutRequestIdIsTolerated() throws Exception {
        Map<String, Object> patch = new HashMap<>();
        patch.put("title", "renamed");
        mvc
            .perform(
                patch("/api/tree/nodes/outcome/{id}", outcome.getId())
                    .with(user(EDITOR))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(patch))
            )
            .andExpect(status().isOk());

        TreeChangeEvent event = capturedEvent();
        assertThat(event.type()).isEqualTo(TreeChangeType.NODE_UPDATED);
        assertThat(event.requestId()).isNull();
    }

    // AC #2 — NODE_MOVED payload is a MoveTreeNodeResponse (node + siblings).
    @Test
    void movePublishesNodeMovedWithSiblings() throws Exception {
        new TransactionTemplate(txMgr).execute(status -> {
            data.opportunity(em.find(Outcome.class, outcome.getId()), null, "Extra", 1);
            return null;
        });
        MoveTreeNodeRequest req = new MoveTreeNodeRequest("opportunity", opportunity.getId(), "outcome", outcome.getId(), 1);
        mvc
            .perform(
                post("/api/tree/nodes/move")
                    .with(user(EDITOR))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(req))
            )
            .andExpect(status().isOk());

        TreeChangeEvent event = capturedEvent();
        assertThat(event.type()).isEqualTo(TreeChangeType.NODE_MOVED);
        assertThat(event.teamId()).isEqualTo(team.getId());
        assertThat(event.payload()).isInstanceOf(MoveTreeNodeResponse.class);
        MoveTreeNodeResponse payload = (MoveTreeNodeResponse) event.payload();
        assertThat(payload.node().getId()).isEqualTo(opportunity.getId());
        assertThat(payload.siblings()).isNotEmpty();
    }

    // AC #2, #8 — delete cascade publishes NODE_DELETED with every descendant key.
    @Test
    void deletePublishesEveryCascadedDescendantKey() throws Exception {
        mvc
            .perform(delete("/api/tree/nodes/opportunity/{id}", opportunity.getId()).with(user(OWNER)).with(csrf()))
            .andExpect(status().isNoContent());

        TreeChangeEvent event = capturedEvent();
        assertThat(event.type()).isEqualTo(TreeChangeType.NODE_DELETED);
        assertThat(event.teamId()).isEqualTo(team.getId());
        assertThat(event.payload()).isInstanceOf(NodeDeletedPayload.class);
        NodeDeletedPayload payload = (NodeDeletedPayload) event.payload();
        assertThat(payload.key()).isEqualTo("opportunity-" + opportunity.getId());
        assertThat(payload.cascadedKeys()).containsExactlyInAnyOrder(
            "solution-" + solution.getId(),
            "assumption-" + assumption.getId(),
            "evidence-" + evidence.getId()
        );
    }

    // AC #8 — every node type is covered by the three services.
    @Test
    void allSixNodeTypesEmitEvents() throws Exception {
        // Create each of outcome, opportunity, solution, assumption, evidence via the tree write API.
        // Product creation is not a tree-write endpoint; a product PATCH exercises the path here.
        createOk("outcome", "product", product.getId(), "cov outcome");
        createOk("opportunity", "outcome", outcome.getId(), "cov opportunity");
        createOk("solution", "opportunity", opportunity.getId(), "cov solution");
        createOk("assumption", "solution", solution.getId(), "cov assumption");
        createOk("evidence", "assumption", assumption.getId(), "cov evidence");
        // Product event via patch.
        mvc
            .perform(
                patch("/api/tree/nodes/product/{id}", product.getId())
                    .with(user(OWNER))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(Map.of("title", "renamed product")))
            )
            .andExpect(status().isOk());

        List<TreeChangeEvent> events = capturedEvents();
        assertThat(events).hasSizeGreaterThanOrEqualTo(6);
        List<TreeNodeType> published = events
            .stream()
            .filter(e -> e.payload() instanceof TreeNodeDTO)
            .map(e -> ((TreeNodeDTO) e.payload()).getType())
            .toList();
        assertThat(published).contains(
            TreeNodeType.OUTCOME,
            TreeNodeType.OPPORTUNITY,
            TreeNodeType.SOLUTION,
            TreeNodeType.ASSUMPTION,
            TreeNodeType.EVIDENCE,
            TreeNodeType.PRODUCT
        );
    }

    // AC #4 — 403 (viewer) publishes nothing.
    @Test
    void viewerRejectionPublishesNothing() throws Exception {
        mvc
            .perform(
                patch("/api/tree/nodes/outcome/{id}", outcome.getId())
                    .with(user(VIEWER))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(Map.of("title", "denied")))
            )
            .andExpect(status().isForbidden());
        verifyNoInteractions(messagingTemplate);
    }

    // AC #4 — 409 (ConcurrencyFailureException thrown from within a tx) publishes nothing.
    @Test
    void concurrencyFailurePublishesNothing() {
        authenticate(EDITOR);
        TransactionTemplate tx = new TransactionTemplate(txMgr);
        try {
            tx.execute(status -> {
                writeService.create(new CreateTreeNodeRequest("outcome", "product", product.getId(), "rolled back"));
                throw new ConcurrencyFailureException("simulated 409");
            });
        } catch (ConcurrencyFailureException expected) {
            // rollback expected
        } finally {
            SecurityContextHolder.clearContext();
        }
        verifyNoInteractions(messagingTemplate);
    }

    // AC #4 — a manually rolled-back tx publishes nothing.
    @Test
    void forcedRollbackPublishesNothing() {
        authenticate(EDITOR);
        TransactionTemplate tx = new TransactionTemplate(txMgr);
        try {
            tx.execute(status -> {
                writeService.create(new CreateTreeNodeRequest("outcome", "product", product.getId(), "forced rollback"));
                status.setRollbackOnly();
                return null;
            });
        } finally {
            SecurityContextHolder.clearContext();
        }
        verifyNoInteractions(messagingTemplate);
    }

    // AC #5 — seq is per-team monotonic and epoch is stable across calls.
    @Test
    void seqIsMonotonicPerTeamAndEpochIsStable() throws Exception {
        createOk("outcome", "product", product.getId(), "one");
        createOk("outcome", "product", product.getId(), "two");
        createOk("outcome", "product", otherProduct.getId(), "three-other");
        createOk("outcome", "product", product.getId(), "four");

        List<TreeChangeEvent> events = capturedEvents();
        assertThat(events).hasSize(4);
        // Every event carries the same epoch.
        String epoch = events.get(0).epoch();
        assertThat(events).allSatisfy(e -> assertThat(e.epoch()).isEqualTo(epoch));
        // seq is strictly increasing within each team.
        List<Long> mainSeqs = events
            .stream()
            .filter(e -> e.teamId().equals(team.getId()))
            .map(TreeChangeEvent::seq)
            .toList();
        List<Long> otherSeqs = events
            .stream()
            .filter(e -> e.teamId().equals(otherTeam.getId()))
            .map(TreeChangeEvent::seq)
            .toList();
        assertThat(mainSeqs).hasSize(3);
        assertThat(mainSeqs).isSorted();
        assertThat(mainSeqs.get(1)).isGreaterThan(mainSeqs.get(0));
        assertThat(mainSeqs.get(2)).isGreaterThan(mainSeqs.get(1));
        assertThat(otherSeqs).hasSize(1);
    }

    // AC #7 — a broadcast failure is caught and never fails the write.
    @Test
    void broadcastFailureDoesNotRollBackTheWrite() throws Exception {
        org.mockito.Mockito.doThrow(new RuntimeException("boom"))
            .when(messagingTemplate)
            .convertAndSend(any(String.class), any(Object.class));
        mvc
            .perform(
                post("/api/tree/nodes")
                    .with(user(EDITOR))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        om.writeValueAsString(new CreateTreeNodeRequest("outcome", "product", product.getId(), "survives broadcast fail"))
                    )
            )
            .andExpect(status().isCreated());
    }

    // Helpers ---------------------------------------------------------------

    private void authenticate(String login) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(login, "x", List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );
    }

    private void createOk(String type, String parentType, Long parentId, String title) throws Exception {
        mvc
            .perform(
                post("/api/tree/nodes")
                    .with(user(OWNER))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(new CreateTreeNodeRequest(type, parentType, parentId, title)))
            )
            .andExpect(status().isCreated());
    }

    /**
     * Captures the single published event and, crucially, asserts the exact destination it went to —
     * {@code /topic/teams/{teamId}/tree} for the event's own team. Matching {@code any(String.class)}
     * would let a broadcast to the wrong (or a wildcard) topic pass unnoticed.
     */
    private TreeChangeEvent capturedEvent() {
        ArgumentCaptor<String> destination = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, times(1)).convertAndSend(destination.capture(), payload.capture());
        TreeChangeEvent event = (TreeChangeEvent) payload.getValue();
        assertThat(destination.getValue()).isEqualTo("/topic/teams/" + event.teamId() + "/tree");
        return event;
    }

    private List<TreeChangeEvent> capturedEvents() {
        ArgumentCaptor<String> destination = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, org.mockito.Mockito.atLeastOnce()).convertAndSend(destination.capture(), payload.capture());
        List<TreeChangeEvent> events = payload
            .getAllValues()
            .stream()
            .map(o -> (TreeChangeEvent) o)
            .toList();
        List<String> destinations = destination.getAllValues();
        for (int i = 0; i < events.size(); i++) {
            assertThat(destinations.get(i)).isEqualTo("/topic/teams/" + events.get(i).teamId() + "/tree");
        }
        return events;
    }
}
