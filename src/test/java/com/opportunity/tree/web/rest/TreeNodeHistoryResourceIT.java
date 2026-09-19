package com.opportunity.tree.web.rest;

import static com.opportunity.tree.web.rest.TreeCollaborationFixture.EDITOR;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.OUTSIDER;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.OWNER;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.VIEWER;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.who;
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

import com.jayway.jsonpath.JsonPath;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.NodeHistory;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link TreeNodeHistoryResource}: the per-node changelog read (FR-H1, FR-H2). */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class TreeNodeHistoryResourceIT {

    private static final String HISTORY = "/api/tree/nodes/{type}/{id}/history";

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

    private void persistHistory(TreeNodeType type, Long id, HistoryEventType event, String summary, Instant at, User author) {
        em.persist(new NodeHistory().nodeType(type).nodeId(id).eventType(event).summary(summary).createdDate(at).author(author));
        em.flush();
    }

    @Test
    void newestFirstWithAuthorAndOnlyThisNode() throws Exception {
        Instant t0 = Instant.parse("2026-09-01T08:00:00Z");
        Long oppId = f.opportunity.getId();
        persistHistory(TreeNodeType.OPPORTUNITY, oppId, HistoryEventType.CREATED, "Node created as opportunity", t0, f.editor);
        persistHistory(
            TreeNodeType.OPPORTUNITY,
            oppId,
            HistoryEventType.STATUS_CHANGED,
            "Status changed to “exploring”",
            t0.plusSeconds(3600),
            f.owner
        );
        persistHistory(TreeNodeType.OPPORTUNITY, oppId, HistoryEventType.VALUE_CHANGED, "Value set to $$$", t0.plusSeconds(60), null);
        // Same id under another type, and another node of the same type: excluded.
        persistHistory(TreeNodeType.SOLUTION, oppId, HistoryEventType.CREATED, "other type", t0, f.owner);
        persistHistory(TreeNodeType.OPPORTUNITY, f.solution.getId(), HistoryEventType.CREATED, "other node", t0, f.owner);

        mvc
            .perform(get(HISTORY, "opportunity", oppId).with(who(VIEWER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[*].eventType", contains("STATUS_CHANGED", "VALUE_CHANGED", "CREATED")))
            .andExpect(
                jsonPath("$[*].summary", contains("Status changed to “exploring”", "Value set to $$$", "Node created as opportunity"))
            )
            .andExpect(jsonPath("$[0].id").isNumber())
            .andExpect(jsonPath("$[0].authorLogin").value(OWNER))
            .andExpect(jsonPath("$[0].authorInitials").value("KP"))
            .andExpect(jsonPath("$[0].createdDate").value("2026-09-01T09:00:00Z"))
            .andExpect(jsonPath("$[1].authorLogin").value(nullValue()))
            .andExpect(jsonPath("$[1].authorInitials").value(nullValue()))
            .andExpect(jsonPath("$[2].authorInitials").value("AR"));
    }

    @Test
    void collaborationEndpointsProduceTheirEntries() throws Exception {
        Long oppId = f.opportunity.getId();
        String link = mvc
            .perform(
                json(
                    post("/api/tree/nodes/{type}/{id}/links", "opportunity", oppId),
                    "{\"name\":\"Jira Epic\",\"url\":\"https://jira.example.com/E-1\"}",
                    EDITOR
                )
            )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long linkId = ((Number) JsonPath.read(link, "$.id")).longValue();
        // Rename writes no history.
        mvc.perform(json(patch("/api/tree/links/{id}", linkId), "{\"name\":\"Epic\"}", EDITOR)).andExpect(status().isOk());
        mvc
            .perform(json(post("/api/tree/opportunities/{id}/questions", oppId), "{\"text\":\"Sized?\"}", EDITOR))
            .andExpect(status().isCreated());
        String comment = mvc
            .perform(json(post("/api/tree/nodes/{type}/{id}/comments", "opportunity", oppId), "{\"body\":\"hi\"}", EDITOR))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long commentId = ((Number) JsonPath.read(comment, "$.id")).longValue();
        // Edit writes no history.
        mvc.perform(json(patch("/api/tree/comments/{id}", commentId), "{\"body\":\"hello\"}", EDITOR)).andExpect(status().isOk());
        mvc.perform(delete("/api/tree/comments/{id}", commentId).with(who(EDITOR)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/tree/links/{id}", linkId).with(who(EDITOR)).with(csrf())).andExpect(status().isNoContent());

        mvc
            .perform(get(HISTORY, "opportunity", oppId).with(who(OWNER)))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$[*].eventType", contains("LINK_REMOVED", "COMMENT_DELETED", "COMMENT_ADDED", "QUESTION_ADDED", "LINK_ADDED"))
            )
            .andExpect(
                jsonPath(
                    "$[*].summary",
                    contains("Link removed", "Comment deleted", "Comment added", "Open question added", "Link added")
                )
            )
            .andExpect(jsonPath("$[*].authorLogin", contains(EDITOR, EDITOR, EDITOR, EDITOR, EDITOR)));
    }

    @Test
    void everyNonProductTypeIsReadableByMembers() throws Exception {
        for (TreeNodeType type : TreeCollaborationFixture.chatTypes()) {
            for (String login : new String[] { OWNER, EDITOR, VIEWER }) {
                mvc
                    .perform(get(HISTORY, TreeCollaborationFixture.path(type), f.idOf(type)).with(who(login)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
            }
        }
    }

    @Test
    void productAndUnknownTypeAre400() throws Exception {
        mvc
            .perform(get(HISTORY, "product", f.product.getId()).with(who(OWNER)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.historynotsupported"));
        mvc
            .perform(get(HISTORY, "interview", f.outcome.getId()).with(who(OWNER)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.unknowntype"));
    }

    @Test
    void nonMemberAdminAndUnknownIdAre403() throws Exception {
        persistHistory(TreeNodeType.OUTCOME, f.outcome.getId(), HistoryEventType.CREATED, "Created", Instant.now(), f.owner);
        mvc.perform(get(HISTORY, "outcome", f.outcome.getId()).with(who(OUTSIDER))).andExpect(status().isForbidden());
        mvc.perform(get(HISTORY, "outcome", f.outcome.getId()).with(who("admin"))).andExpect(status().isForbidden());
        mvc.perform(get(HISTORY, "evidence", Long.MAX_VALUE).with(who(OWNER))).andExpect(status().isForbidden());
        mvc.perform(get(HISTORY, "outcome", f.outcome.getId())).andExpect(status().isUnauthorized());
    }
}
