package com.opportunity.tree.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Comment;
import com.opportunity.tree.domain.NodeLink;
import com.opportunity.tree.domain.OpenQuestion;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.service.TreeCommentService;
import com.opportunity.tree.service.broadcast.CommentChangedPayload;
import com.opportunity.tree.service.broadcast.CommentDeletedPayload;
import com.opportunity.tree.service.broadcast.LinkChangedPayload;
import com.opportunity.tree.service.broadcast.LinkRemovedPayload;
import com.opportunity.tree.service.broadcast.MembershipChangedPayload;
import com.opportunity.tree.service.broadcast.QuestionChangedPayload;
import com.opportunity.tree.service.broadcast.QuestionRemovedPayload;
import com.opportunity.tree.service.broadcast.TreeChangeEvent;
import com.opportunity.tree.service.broadcast.TreeChangePublisher;
import com.opportunity.tree.service.broadcast.TreeChangeType;
import com.opportunity.tree.service.dto.AddTeamMemberRequest;
import com.opportunity.tree.service.dto.ChangeTeamMemberRoleRequest;
import com.opportunity.tree.service.dto.tree.TreeCommentDTO;
import com.opportunity.tree.service.dto.tree.TreeCommentWriteDTO;
import com.opportunity.tree.service.dto.tree.TreeLinkWriteDTO;
import com.opportunity.tree.service.dto.tree.TreeQuestionWriteDTO;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
 * Integration tests for RTC-002 — publish after-commit events for link, open-question, comment and
 * membership writes through the RTC-001 publisher. Verifies the payload shape (node key + DTO or
 * id, plus the new {@code commentCount} for comment events), the same envelope
 * ({@code seq}/{@code epoch}/{@code actingUserLogin}/{@code requestId}), and that no event is
 * published for a rejected write.
 */
@IntegrationTest
@AutoConfigureMockMvc
class TreeCollaborationBroadcastIT {

    private static final String OWNER = "rtc2-owner";
    private static final String EDITOR = "rtc2-editor";
    private static final String VIEWER = "rtc2-viewer";
    private static final String OUTSIDER = "rtc2-outsider";

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private EntityManager em;

    @Autowired
    private TreeCommentService commentService;

    @Autowired
    private TreeChangePublisher publisher;

    @Autowired
    private PlatformTransactionManager txMgr;

    private OstTreeTestData data;
    private Team team;
    private Product product;
    private Outcome outcome;
    private Opportunity opportunity;
    private User outsider;

    @BeforeEach
    void setUp() {
        new TransactionTemplate(txMgr).execute(status -> {
            data = new OstTreeTestData(em);
            team = data.team("RTC2 Team");
            data.member(team, data.user(OWNER), TeamRole.OWNER);
            data.member(team, data.user(EDITOR), TeamRole.EDITOR);
            data.member(team, data.user(VIEWER), TeamRole.VIEWER);
            outsider = data.user(OUTSIDER);
            product = data.product(team, "RTC2 product", 0);
            outcome = data.outcome(product, "RTC2 outcome", 0);
            opportunity = data.opportunity(outcome, null, "RTC2 opportunity", 0);
            em.flush();
            return null;
        });
    }

    /**
     * This IT is intentionally non-transactional (the AFTER_COMMIT broadcast only fires on a real
     * commit), so everything {@link #setUp()} seeds is committed and would otherwise survive into
     * later tests in the same JVM — the leftover {@code team_member} rows made
     * {@code UserResourceIT.testUserEquals} fail with {@code fk_team_member__user_id}. Remove
     * exactly what this class created, in FK-safe order.
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        List<Long> teamIds = team == null || team.getId() == null ? List.of() : List.of(team.getId());
        OstTreeTestCleanup.removeTeamsAndUsers(txMgr, em, teamIds, List.of(OWNER, EDITOR, VIEWER, OUTSIDER));
    }

    // AC #1, #4 — adding a link publishes LINK_ADDED with node key + TreeLinkDTO and the envelope.
    @Test
    void addLinkPublishesLinkAdded() throws Exception {
        mvc
            .perform(
                post("/api/tree/nodes/opportunity/{id}/links", opportunity.getId())
                    .with(user(EDITOR))
                    .with(csrf())
                    .header(TreeChangePublisher.REQUEST_ID_HEADER, "req-link-add")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(new TreeLinkWriteDTO("Design", "https://example.com/design")))
            )
            .andExpect(status().isCreated());

        TreeChangeEvent event = capturedEvent();
        assertThat(event.type()).isEqualTo(TreeChangeType.LINK_ADDED);
        assertThat(event.teamId()).isEqualTo(team.getId());
        assertThat(event.actingUserLogin()).isEqualTo(EDITOR);
        assertThat(event.seq()).isPositive();
        assertThat(event.epoch()).isEqualTo(publisher.epoch());
        assertThat(event.requestId()).isEqualTo("req-link-add");
        assertThat(event.payload()).isInstanceOf(LinkChangedPayload.class);
        LinkChangedPayload payload = (LinkChangedPayload) event.payload();
        assertThat(payload.nodeKey()).isEqualTo("opportunity-" + opportunity.getId());
        assertThat(payload.link().name()).isEqualTo("Design");
        assertThat(payload.link().url()).isEqualTo("https://example.com/design");
    }

    // AC #1 — LINK_REMOVED carries the owning node key and the link id.
    @Test
    void deleteLinkPublishesLinkRemoved() throws Exception {
        NodeLink existing = seedLinkOn(opportunity);

        mvc.perform(delete("/api/tree/links/{id}", existing.getId()).with(user(EDITOR)).with(csrf())).andExpect(status().isNoContent());

        TreeChangeEvent event = capturedEvent();
        assertThat(event.type()).isEqualTo(TreeChangeType.LINK_REMOVED);
        assertThat(event.payload()).isInstanceOf(LinkRemovedPayload.class);
        LinkRemovedPayload payload = (LinkRemovedPayload) event.payload();
        assertThat(payload.nodeKey()).isEqualTo("opportunity-" + opportunity.getId());
        assertThat(payload.linkId()).isEqualTo(existing.getId());
    }

    // AC #2, #4 — question publish carries node key + TreeQuestionDTO.
    @Test
    void addQuestionPublishesQuestionAdded() throws Exception {
        mvc
            .perform(
                post("/api/tree/opportunities/{id}/questions", opportunity.getId())
                    .with(user(EDITOR))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(new TreeQuestionWriteDTO("Who benefits most?", null)))
            )
            .andExpect(status().isCreated());

        TreeChangeEvent event = capturedEvent();
        assertThat(event.type()).isEqualTo(TreeChangeType.QUESTION_ADDED);
        assertThat(event.teamId()).isEqualTo(team.getId());
        assertThat(event.payload()).isInstanceOf(QuestionChangedPayload.class);
        QuestionChangedPayload payload = (QuestionChangedPayload) event.payload();
        assertThat(payload.nodeKey()).isEqualTo("opportunity-" + opportunity.getId());
        assertThat(payload.question().text()).isEqualTo("Who benefits most?");
    }

    // AC #2 — QUESTION_REMOVED carries the node key and the question id.
    @Test
    void deleteQuestionPublishesQuestionRemoved() throws Exception {
        OpenQuestion q = seedQuestionOn(opportunity);

        mvc.perform(delete("/api/tree/questions/{id}", q.getId()).with(user(EDITOR)).with(csrf())).andExpect(status().isNoContent());

        TreeChangeEvent event = capturedEvent();
        assertThat(event.type()).isEqualTo(TreeChangeType.QUESTION_REMOVED);
        assertThat(event.payload()).isInstanceOf(QuestionRemovedPayload.class);
        QuestionRemovedPayload payload = (QuestionRemovedPayload) event.payload();
        assertThat(payload.nodeKey()).isEqualTo("opportunity-" + opportunity.getId());
        assertThat(payload.questionId()).isEqualTo(q.getId());
    }

    // AC #3, #4 — COMMENT_ADDED carries node key, the TreeCommentDTO and the node's new commentCount.
    @Test
    void addCommentPublishesCommentAddedWithCount() throws Exception {
        mvc
            .perform(
                post("/api/tree/nodes/opportunity/{id}/comments", opportunity.getId())
                    .with(user(EDITOR))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(new TreeCommentWriteDTO("hello world")))
            )
            .andExpect(status().isCreated());

        TreeChangeEvent event = capturedEvent();
        assertThat(event.type()).isEqualTo(TreeChangeType.COMMENT_ADDED);
        assertThat(event.teamId()).isEqualTo(team.getId());
        assertThat(event.payload()).isInstanceOf(CommentChangedPayload.class);
        CommentChangedPayload payload = (CommentChangedPayload) event.payload();
        assertThat(payload.nodeKey()).isEqualTo("opportunity-" + opportunity.getId());
        assertThat(payload.comment().body()).isEqualTo("hello world");
        assertThat(payload.commentCount()).isEqualTo(1L);
    }

    // AC #3 — COMMENT_DELETED carries node key, id, and the new (post-delete) commentCount.
    @Test
    void deleteCommentPublishesCommentDeletedWithCount() throws Exception {
        // Seed via the service so the author matches the acting user.
        authenticate(EDITOR);
        Long commentId;
        try {
            TreeCommentDTO added = commentService.addComment(
                com.opportunity.tree.domain.enumeration.TreeNodeType.OPPORTUNITY,
                opportunity.getId(),
                new TreeCommentWriteDTO("first")
            );
            commentId = added.id();
        } finally {
            SecurityContextHolder.clearContext();
        }
        // Reset the mock so we only capture the DELETE event.
        org.mockito.Mockito.reset(messagingTemplate);

        mvc.perform(delete("/api/tree/comments/{id}", commentId).with(user(EDITOR)).with(csrf())).andExpect(status().isNoContent());

        TreeChangeEvent event = capturedEvent();
        assertThat(event.type()).isEqualTo(TreeChangeType.COMMENT_DELETED);
        assertThat(event.payload()).isInstanceOf(CommentDeletedPayload.class);
        CommentDeletedPayload payload = (CommentDeletedPayload) event.payload();
        assertThat(payload.nodeKey()).isEqualTo("opportunity-" + opportunity.getId());
        assertThat(payload.commentId()).isEqualTo(commentId);
        assertThat(payload.commentCount()).isEqualTo(0L);
    }

    // AC #5 — role change publishes MEMBERSHIP_CHANGED with the affected login and the new role.
    @Test
    void changeRolePublishesMembershipChanged() throws Exception {
        // Add outsider as EDITOR first (this itself does not currently publish, but reset to be safe).
        addMember(outsider.getId(), TeamRole.EDITOR);
        org.mockito.Mockito.reset(messagingTemplate);

        ChangeTeamMemberRoleRequest req = new ChangeTeamMemberRoleRequest();
        req.setRole(TeamRole.VIEWER);
        mvc
            .perform(
                put("/api/team-management/teams/{id}/members/{userId}", team.getId(), outsider.getId())
                    .with(user(OWNER))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(req))
            )
            .andExpect(status().isOk());

        TreeChangeEvent event = capturedEvent();
        assertThat(event.type()).isEqualTo(TreeChangeType.MEMBERSHIP_CHANGED);
        assertThat(event.teamId()).isEqualTo(team.getId());
        assertThat(event.payload()).isInstanceOf(MembershipChangedPayload.class);
        MembershipChangedPayload payload = (MembershipChangedPayload) event.payload();
        assertThat(payload.login()).isEqualTo(OUTSIDER);
        assertThat(payload.role()).isEqualTo(TeamRole.VIEWER);
        assertThat(payload.removed()).isFalse();
    }

    // AC #5, #6 — removing a member publishes MEMBERSHIP_CHANGED with removed=true.
    @Test
    void removeMemberPublishesMembershipRemoved() throws Exception {
        addMember(outsider.getId(), TeamRole.EDITOR);
        org.mockito.Mockito.reset(messagingTemplate);

        mvc
            .perform(
                delete("/api/team-management/teams/{id}/members/{userId}", team.getId(), outsider.getId()).with(user(OWNER)).with(csrf())
            )
            .andExpect(status().isNoContent());

        TreeChangeEvent event = capturedEvent();
        assertThat(event.type()).isEqualTo(TreeChangeType.MEMBERSHIP_CHANGED);
        assertThat(event.teamId()).isEqualTo(team.getId());
        MembershipChangedPayload payload = (MembershipChangedPayload) event.payload();
        assertThat(payload.login()).isEqualTo(OUTSIDER);
        assertThat(payload.removed()).isTrue();
        assertThat(payload.role()).isNull();
    }

    // AC #7 — a viewer (403) trying to add a link publishes nothing.
    @Test
    void viewerLinkAddRejectionPublishesNothing() throws Exception {
        mvc
            .perform(
                post("/api/tree/nodes/opportunity/{id}/links", opportunity.getId())
                    .with(user(VIEWER))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(new TreeLinkWriteDTO("nope", "https://example.com/")))
            )
            .andExpect(status().isForbidden());
        verifyNoInteractions(messagingTemplate);
    }

    // AC #7 — an invalid comment body (400) publishes nothing.
    @Test
    void invalidCommentBodyPublishesNothing() throws Exception {
        mvc
            .perform(
                post("/api/tree/nodes/opportunity/{id}/comments", opportunity.getId())
                    .with(user(EDITOR))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(new TreeCommentWriteDTO("   ")))
            )
            .andExpect(status().is4xxClientError());
        verifyNoInteractions(messagingTemplate);
    }

    // AC #7 — a non-owner (403) attempting to change a role publishes nothing.
    @Test
    void nonOwnerRoleChangeRejectionPublishesNothing() throws Exception {
        addMember(outsider.getId(), TeamRole.EDITOR);
        org.mockito.Mockito.reset(messagingTemplate);

        ChangeTeamMemberRoleRequest req = new ChangeTeamMemberRoleRequest();
        req.setRole(TeamRole.VIEWER);
        mvc
            .perform(
                put("/api/team-management/teams/{id}/members/{userId}", team.getId(), outsider.getId())
                    .with(user(EDITOR))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(req))
            )
            .andExpect(status().isForbidden());
        verifyNoInteractions(messagingTemplate);
    }

    // Helpers ---------------------------------------------------------------

    private void authenticate(String login) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(login, "x", List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );
    }

    private void addMember(String userId, TeamRole role) throws Exception {
        AddTeamMemberRequest add = new AddTeamMemberRequest();
        add.setUserId(userId);
        add.setRole(role);
        mvc
            .perform(
                post("/api/team-management/teams/{id}/members", team.getId())
                    .with(user(OWNER))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(add))
            )
            .andExpect(status().isCreated());
    }

    private NodeLink seedLinkOn(Opportunity op) {
        return new TransactionTemplate(txMgr).execute(status -> {
            NodeLink l = new NodeLink().name("Existing").url("https://example.com/existing").sortOrder(0).createdDate(Instant.now());
            l.setOpportunity(em.getReference(Opportunity.class, op.getId()));
            em.persist(l);
            em.flush();
            return l;
        });
    }

    private OpenQuestion seedQuestionOn(Opportunity op) {
        return new TransactionTemplate(txMgr).execute(status -> {
            OpenQuestion q = new OpenQuestion()
                .questionText("Existing?")
                .done(false)
                .sortOrder(0)
                .createdDate(Instant.now())
                .opportunity(em.getReference(Opportunity.class, op.getId()));
            em.persist(q);
            em.flush();
            return q;
        });
    }

    @SuppressWarnings("unused")
    private Comment lastComment() {
        return em.createQuery("select c from Comment c order by c.id desc", Comment.class).setMaxResults(1).getSingleResult();
    }

    private TreeChangeEvent capturedEvent() {
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(any(String.class), payload.capture());
        List<Object> values = payload.getAllValues();
        return (TreeChangeEvent) values.get(values.size() - 1);
    }
}
