package com.opportunity.tree.web.rest;

import static com.opportunity.tree.web.rest.TreeCollaborationFixture.EDITOR;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.OUTSIDER;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.OWNER;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.VIEWER;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.path;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.who;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.NodeHistory;
import com.opportunity.tree.domain.NodeLink;
import com.opportunity.tree.domain.enumeration.HistoryEventType;
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

/** Integration tests for {@link TreeNodeLinkResource}: links on all six node types (FR-D4). */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class TreeNodeLinkResourceIT {

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

    private NodeLink persistLink(String name, int sortOrder) {
        NodeLink l = new NodeLink()
            .name(name)
            .url("https://example.com/" + name)
            .sortOrder(sortOrder)
            .createdDate(Instant.now())
            .opportunity(f.opportunity);
        em.persist(l);
        em.flush();
        return l;
    }

    @Test
    void editorAddsLinkToEveryNodeTypeWithHistory() throws Exception {
        for (TreeNodeType type : TreeNodeType.values()) {
            Long id = f.idOf(type);
            mvc
                .perform(
                    json(
                        post("/api/tree/nodes/{type}/{id}/links", path(type), id),
                        "{\"name\":\" Confluence \",\"url\":\"https://wiki.example.com/x\"}",
                        EDITOR
                    )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Confluence"))
                .andExpect(jsonPath("$.url").value("https://wiki.example.com/x"))
                .andExpect(jsonPath("$.sortOrder").value(0));

            List<NodeHistory> h = history(type, id);
            assertThat(h).hasSize(1);
            assertThat(h.get(0).getEventType()).isEqualTo(HistoryEventType.LINK_ADDED);
            assertThat(h.get(0).getSummary()).isEqualTo("Linked to Confluence");
            assertThat(h.get(0).getAuthor().getLogin()).isEqualTo(EDITOR);
        }
        String col = "select count(l) from NodeLink l where l.%s.id = ?1";
        assertThat(f.count(col.formatted("product"), f.product.getId())).isEqualTo(1);
        assertThat(f.count(col.formatted("evidence"), f.evidence.getId())).isEqualTo(1);
    }

    @Test
    void sortOrderIsMaxPlusOneAndTypeIsCaseInsensitive() throws Exception {
        persistLink("a", 0);
        persistLink("b", 4);
        mvc
            .perform(
                json(
                    post("/api/tree/nodes/{type}/{id}/links", "OPPORTUNITY", f.opportunity.getId()),
                    "{\"name\":\"Jira\",\"url\":\"http://jira.example.com\"}",
                    OWNER
                )
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sortOrder").value(5));
    }

    @Test
    void invalidInputIs400() throws Exception {
        String url = "/api/tree/nodes/{type}/{id}/links";
        Long id = f.outcome.getId();
        mvc
            .perform(json(post(url, "outcome", id), "{\"name\":\"x\",\"url\":\"ftp://x\"}", OWNER))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.linkurlinvalid"));
        mvc
            .perform(json(post(url, "outcome", id), "{\"name\":\"x\",\"url\":\"https://\"}", OWNER))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.linkurlinvalid"));
        mvc
            .perform(json(post(url, "outcome", id), "{\"name\":\"   \",\"url\":\"https://x\"}", OWNER))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.linknameinvalid"));
        mvc
            .perform(json(post(url, "outcome", id), "{\"name\":\"" + "n".repeat(101) + "\",\"url\":\"https://x\"}", OWNER))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.linknameinvalid"));
        mvc
            .perform(json(post(url, "outcome", id), "{\"name\":\"x\",\"url\":\"https://" + "u".repeat(2000) + "\"}", OWNER))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.linkurlinvalid"));
        mvc
            .perform(json(post(url, "banana", id), "{\"name\":\"x\",\"url\":\"https://x\"}", OWNER))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.nodetypeinvalid"));
        assertThat(f.linkCount()).isZero();
        assertThat(f.historyCount()).isZero();
    }

    @Test
    void editRenamesAndRepointsWithoutHistory() throws Exception {
        NodeLink link = persistLink("Old", 2);
        mvc
            .perform(json(patch("/api/tree/links/{id}", link.getId()), "{\"name\":\"New\"}", EDITOR))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("New"))
            .andExpect(jsonPath("$.url").value("https://example.com/Old"))
            .andExpect(jsonPath("$.sortOrder").value(2));
        mvc
            .perform(json(patch("/api/tree/links/{id}", link.getId()), "{\"url\":\"https://new.example.com\"}", OWNER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("New"))
            .andExpect(jsonPath("$.url").value("https://new.example.com"));
        mvc
            .perform(json(patch("/api/tree/links/{id}", link.getId()), "{\"url\":\"nope\"}", OWNER))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.linkurlinvalid"));
        assertThat(history(TreeNodeType.OPPORTUNITY, f.opportunity.getId())).isEmpty();
    }

    @Test
    void removeRecordsHistory() throws Exception {
        NodeLink link = persistLink("Jira Epic", 0);
        mvc.perform(delete("/api/tree/links/{id}", link.getId()).with(user(EDITOR)).with(csrf())).andExpect(status().isNoContent());
        assertThat(f.count("select count(l) from NodeLink l where l.id = ?1", link.getId())).isZero();
        List<NodeHistory> h = history(TreeNodeType.OPPORTUNITY, f.opportunity.getId());
        assertThat(h).hasSize(1);
        assertThat(h.get(0).getEventType()).isEqualTo(HistoryEventType.LINK_REMOVED);
        assertThat(h.get(0).getSummary()).isEqualTo("Removed link “Jira Epic”");
    }

    @Test
    void viewerNonMemberAndAdminAreForbiddenAndNothingChanges() throws Exception {
        NodeLink link = persistLink("Keep", 0);
        for (String login : List.of(VIEWER, OUTSIDER, "admin")) {
            mvc
                .perform(
                    json(
                        post("/api/tree/nodes/{type}/{id}/links", "opportunity", f.opportunity.getId()),
                        "{\"name\":\"x\",\"url\":\"https://x\"}",
                        login
                    )
                )
                .andExpect(status().isForbidden());
            mvc
                .perform(json(patch("/api/tree/links/{id}", link.getId()), "{\"name\":\"Hacked\"}", login))
                .andExpect(status().isForbidden());
            mvc.perform(delete("/api/tree/links/{id}", link.getId()).with(who(login)).with(csrf())).andExpect(status().isForbidden());
        }
        // Unknown ids look exactly like foreign ones, even to an owner.
        mvc
            .perform(
                json(post("/api/tree/nodes/{type}/{id}/links", "solution", Long.MAX_VALUE), "{\"name\":\"x\",\"url\":\"https://x\"}", OWNER)
            )
            .andExpect(status().isForbidden());
        mvc.perform(json(patch("/api/tree/links/{id}", Long.MAX_VALUE), "{\"name\":\"x\"}", OWNER)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/tree/links/{id}", Long.MAX_VALUE).with(user(OWNER)).with(csrf())).andExpect(status().isForbidden());

        em.clear();
        assertThat(em.find(NodeLink.class, link.getId()).getName()).isEqualTo("Keep");
        assertThat(f.linkCount()).isEqualTo(1);
        assertThat(f.historyCount()).isZero();
    }

    @Test
    void unauthenticatedIs401() throws Exception {
        mvc
            .perform(
                post("/api/tree/nodes/{type}/{id}/links", "outcome", f.outcome.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"x\",\"url\":\"https://x\"}")
                    .with(csrf())
            )
            .andExpect(status().isUnauthorized());
    }
}
