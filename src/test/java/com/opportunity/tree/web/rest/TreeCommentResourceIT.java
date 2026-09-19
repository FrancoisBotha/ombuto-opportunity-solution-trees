package com.opportunity.tree.web.rest;

import static com.opportunity.tree.web.rest.TreeCollaborationFixture.EDITOR;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.OUTSIDER;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.OWNER;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.VIEWER;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.path;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.who;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Comment;
import com.opportunity.tree.domain.NodeHistory;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link TreeCommentResource}: per-node chat (FR-M1..M3, amendment A4). */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class TreeCommentResourceIT {

    private static final String COMMENTS = "/api/tree/nodes/{type}/{id}/comments";

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc mvc;

    private TreeCollaborationFixture f;

    @BeforeEach
    void setUp() {
        f = new TreeCollaborationFixture(em);
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder b, String body, String login) {
        return b.contentType(MediaType.APPLICATION_JSON).content(body).with(who(login)).with(csrf());
    }

    private List<NodeHistory> history(TreeNodeType type, Long id) {
        return em
            .createQuery("select h from NodeHistory h where h.nodeType = :t and h.nodeId = :id order by h.id", NodeHistory.class)
            .setParameter("t", type)
            .setParameter("id", id)
            .getResultList();
    }

    private Comment persistComment(User author, String body, Instant at) {
        Comment c = new Comment().body(body).createdDate(at).author(author).opportunity(f.opportunity);
        em.persist(c);
        em.flush();
        return c;
    }

    private long commentCountOnOpportunity() {
        return f.count("select count(c) from Comment c where c.opportunity.id = ?1", f.opportunity.getId());
    }

    @Test
    void editorPostsOnEveryChatNodeTypeWithHistory() throws Exception {
        for (TreeNodeType type : TreeCollaborationFixture.chatTypes()) {
            Long id = f.idOf(type);
            mvc
                .perform(json(post(COMMENTS, path(type), id), "{\"body\":\"  Hello there \\n\"}", EDITOR))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.body").value("Hello there"))
                .andExpect(jsonPath("$.authorLogin").value(EDITOR))
                .andExpect(jsonPath("$.authorInitials").value("AR"))
                .andExpect(jsonPath("$.authorName").value("Ari Reyes"))
                .andExpect(jsonPath("$.createdDate").isNotEmpty())
                .andExpect(jsonPath("$.editedDate").value(nullValue()))
                .andExpect(jsonPath("$.mine").value(true));

            mvc
                .perform(get(COMMENTS, path(type), id).with(who(VIEWER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].body").value("Hello there"))
                .andExpect(jsonPath("$[0].mine").value(false));

            List<NodeHistory> h = history(type, id);
            assertThat(h).hasSize(1);
            assertThat(h.get(0).getEventType()).isEqualTo(HistoryEventType.COMMENT_ADDED);
            assertThat(h.get(0).getSummary()).isEqualTo("Comment added");
        }
        assertThat(f.count("select count(c) from Comment c where c.assumption.id = ?1", f.assumption.getId())).isEqualTo(1);
        assertThat(f.count("select count(c) from Comment c where c.evidence.id = ?1", f.evidence.getId())).isEqualTo(1);
    }

    @Test
    void threadIsOldestFirstWithMineFlag() throws Exception {
        Instant t0 = Instant.parse("2026-09-01T10:00:00Z");
        persistComment(f.owner, "second", t0.plusSeconds(60));
        persistComment(f.editor, "first", t0);
        persistComment(f.owner, "third", t0.plusSeconds(120));

        mvc
            .perform(get(COMMENTS, "opportunity", f.opportunity.getId()).with(who(OWNER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].body", contains("first", "second", "third")))
            .andExpect(jsonPath("$[*].mine", contains(false, true, true)))
            .andExpect(jsonPath("$[*].authorInitials", contains("AR", "KP", "KP")))
            .andExpect(jsonPath("$[0].authorName").value("Ari Reyes"));
    }

    @Test
    void authorEditsOwnMessageSetsEditedDateWithoutHistory() throws Exception {
        Comment c = persistComment(f.editor, "typo", Instant.now());
        mvc
            .perform(json(patch("/api/tree/comments/{id}", c.getId()), "{\"body\":\"fixed\"}", EDITOR))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.body").value("fixed"))
            .andExpect(jsonPath("$.editedDate").isNotEmpty())
            .andExpect(jsonPath("$.mine").value(true));
        em.flush();
        em.clear();
        Comment reloaded = em.find(Comment.class, c.getId());
        assertThat(reloaded.getBody()).isEqualTo("fixed");
        assertThat(reloaded.getEditedDate()).isNotNull();
        assertThat(history(TreeNodeType.OPPORTUNITY, f.opportunity.getId())).isEmpty();
    }

    /** Edit + delete by comment id on every chat node type: exercises the comment → node column mapping in TeamAccessService. */
    @Test
    void editAndDeleteByIdWorkOnEveryChatNodeType() throws Exception {
        for (TreeNodeType type : TreeCollaborationFixture.chatTypes()) {
            Comment c = new Comment().body("on " + type).createdDate(Instant.now()).author(f.editor);
            switch (type) {
                case OUTCOME -> c.outcome(f.outcome);
                case OPPORTUNITY -> c.opportunity(f.opportunity);
                case SOLUTION -> c.solution(f.solution);
                case ASSUMPTION -> c.assumption(f.assumption);
                case EVIDENCE -> c.evidence(f.evidence);
                default -> throw new IllegalStateException(type.name());
            }
            em.persist(c);
            em.flush();

            mvc
                .perform(json(patch("/api/tree/comments/{id}", c.getId()), "{\"body\":\"not yours\"}", OWNER))
                .andExpect(status().isForbidden());
            mvc
                .perform(json(patch("/api/tree/comments/{id}", c.getId()), "{\"body\":\"edited\"}", EDITOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("edited"));
            mvc.perform(delete("/api/tree/comments/{id}", c.getId()).with(who(OUTSIDER)).with(csrf())).andExpect(status().isForbidden());
            mvc.perform(delete("/api/tree/comments/{id}", c.getId()).with(who(EDITOR)).with(csrf())).andExpect(status().isNoContent());

            assertThat(f.count("select count(c) from Comment c where c.id = ?1", c.getId())).as(type.name()).isZero();
            assertThat(history(type, f.idOf(type))).as(type.name()).extracting(NodeHistory::getSummary).containsExactly("Comment deleted");
        }
    }

    @Test
    void authorDeletesOwnMessageWithHistory() throws Exception {
        Comment c = persistComment(f.owner, "bye", Instant.now());
        Comment other = persistComment(f.editor, "still here", Instant.now());

        mvc.perform(delete("/api/tree/comments/{id}", c.getId()).with(who(OWNER)).with(csrf())).andExpect(status().isNoContent());

        assertThat(f.count("select count(c) from Comment c where c.id = ?1", c.getId())).isZero();
        assertThat(f.count("select count(c) from Comment c where c.id = ?1", other.getId())).isEqualTo(1);
        // The thread is flat: deleting one message leaves exactly the others, in order.
        mvc
            .perform(get(COMMENTS, "opportunity", f.opportunity.getId()).with(who(OWNER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(other.getId()))
            .andExpect(jsonPath("$[0].body").value("still here"));
        List<NodeHistory> h = history(TreeNodeType.OPPORTUNITY, f.opportunity.getId());
        assertThat(h).hasSize(1);
        assertThat(h.get(0).getEventType()).isEqualTo(HistoryEventType.COMMENT_DELETED);
        assertThat(h.get(0).getSummary()).isEqualTo("Comment deleted");
        assertThat(h.get(0).getAuthor().getLogin()).isEqualTo(OWNER);
    }

    @Test
    void onlyTheAuthorCanEditOrDeleteEvenAnOwner() throws Exception {
        Comment c = persistComment(f.editor, "mine", Instant.now());
        mvc.perform(json(patch("/api/tree/comments/{id}", c.getId()), "{\"body\":\"hijack\"}", OWNER)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/tree/comments/{id}", c.getId()).with(who(OWNER)).with(csrf())).andExpect(status().isForbidden());
        em.clear();
        assertThat(em.find(Comment.class, c.getId()).getBody()).isEqualTo("mine");
        assertThat(history(TreeNodeType.OPPORTUNITY, f.opportunity.getId())).isEmpty();
    }

    @Test
    void authorDemotedToViewerCanNoLongerEditOrDelete() throws Exception {
        Comment c = persistComment(f.editor, "mine", Instant.now());
        em
            .createQuery("update TeamMember tm set tm.role = :r where tm.user.id = :u and tm.team.id = :t")
            .setParameter("r", TeamRole.VIEWER)
            .setParameter("u", f.editor.getId())
            .setParameter("t", f.team.getId())
            .executeUpdate();
        em.clear();

        mvc.perform(json(patch("/api/tree/comments/{id}", c.getId()), "{\"body\":\"edit\"}", EDITOR)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/tree/comments/{id}", c.getId()).with(who(EDITOR)).with(csrf())).andExpect(status().isForbidden());
        // Still readable; "mine" reflects authorship only (the client hides edit controls for viewers).
        mvc
            .perform(get(COMMENTS, "opportunity", f.opportunity.getId()).with(who(EDITOR)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].mine").value(true));
        assertThat(commentCountOnOpportunity()).isEqualTo(1);
    }

    @Test
    void viewerReadsButCannotPost() throws Exception {
        persistComment(f.owner, "hello", Instant.now());
        mvc
            .perform(get(COMMENTS, "opportunity", f.opportunity.getId()).with(who(VIEWER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
        mvc
            .perform(json(post(COMMENTS, "opportunity", f.opportunity.getId()), "{\"body\":\"hi\"}", VIEWER))
            .andExpect(status().isForbidden());
        assertThat(commentCountOnOpportunity()).isEqualTo(1);
        assertThat(history(TreeNodeType.OPPORTUNITY, f.opportunity.getId())).isEmpty();
    }

    @Test
    void nonMemberAndAdminCannotReadPostEditOrDelete() throws Exception {
        Comment c = persistComment(f.owner, "secret", Instant.now());
        for (String login : List.of(OUTSIDER, "admin")) {
            mvc.perform(get(COMMENTS, "opportunity", f.opportunity.getId()).with(who(login))).andExpect(status().isForbidden());
            mvc
                .perform(json(post(COMMENTS, "opportunity", f.opportunity.getId()), "{\"body\":\"x\"}", login))
                .andExpect(status().isForbidden());
            mvc.perform(json(patch("/api/tree/comments/{id}", c.getId()), "{\"body\":\"x\"}", login)).andExpect(status().isForbidden());
            mvc.perform(delete("/api/tree/comments/{id}", c.getId()).with(who(login)).with(csrf())).andExpect(status().isForbidden());
        }
        // Unknown ids: same 403.
        mvc.perform(get(COMMENTS, "outcome", Long.MAX_VALUE).with(who(OWNER))).andExpect(status().isForbidden());
        mvc.perform(json(patch("/api/tree/comments/{id}", Long.MAX_VALUE), "{\"body\":\"x\"}", OWNER)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/tree/comments/{id}", Long.MAX_VALUE).with(who(OWNER)).with(csrf())).andExpect(status().isForbidden());
        assertThat(commentCountOnOpportunity()).isEqualTo(1);
    }

    @Test
    void productHasNoChatAndBadInputIs400() throws Exception {
        mvc
            .perform(get(COMMENTS, "product", f.product.getId()).with(who(OWNER)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.chatnotsupported"));
        mvc
            .perform(json(post(COMMENTS, "product", f.product.getId()), "{\"body\":\"x\"}", OWNER))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.chatnotsupported"));
        mvc
            .perform(get(COMMENTS, "experiment", f.outcome.getId()).with(who(OWNER)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.unknowntype"));
        mvc
            .perform(json(post(COMMENTS, "outcome", f.outcome.getId()), "{\"body\":\"   \"}", OWNER))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.commentbodyinvalid"));
        Comment c = persistComment(f.owner, "keep", Instant.now());
        mvc
            .perform(json(patch("/api/tree/comments/{id}", c.getId()), "{\"body\":\"\"}", OWNER))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.commentbodyinvalid"));
        assertThat(f.historyCount()).isZero();
    }
}
