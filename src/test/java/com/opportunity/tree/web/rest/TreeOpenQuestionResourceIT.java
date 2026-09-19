package com.opportunity.tree.web.rest;

import static com.opportunity.tree.web.rest.TreeCollaborationFixture.EDITOR;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.OUTSIDER;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.OWNER;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.VIEWER;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.who;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.NodeHistory;
import com.opportunity.tree.domain.OpenQuestion;
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

/** Integration tests for {@link TreeOpenQuestionResource}: the opportunity open-question checklist (FR-Q1). */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class TreeOpenQuestionResourceIT {

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

    private List<NodeHistory> history() {
        return em
            .createQuery("select h from NodeHistory h where h.nodeType = :t and h.nodeId = :id order by h.id", NodeHistory.class)
            .setParameter("t", TreeNodeType.OPPORTUNITY)
            .setParameter("id", f.opportunity.getId())
            .getResultList();
    }

    private OpenQuestion persistQuestion(String text, int sortOrder) {
        OpenQuestion q = new OpenQuestion()
            .questionText(text)
            .done(false)
            .sortOrder(sortOrder)
            .createdDate(Instant.now())
            .opportunity(f.opportunity);
        em.persist(q);
        em.flush();
        return q;
    }

    private long questionCount() {
        return f.count("select count(q) from OpenQuestion q where q.opportunity.id = ?1", f.opportunity.getId());
    }

    @Test
    void editorAddsQuestionAtTheEndWithHistory() throws Exception {
        persistQuestion("First?", 3);
        mvc
            .perform(json(post("/api/tree/opportunities/{id}/questions", f.opportunity.getId()), "{\"text\":\" Who else? \"}", EDITOR))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.text").value("Who else?"))
            .andExpect(jsonPath("$.done").value(false))
            .andExpect(jsonPath("$.sortOrder").value(4));

        List<NodeHistory> h = history();
        assertThat(h).hasSize(1);
        assertThat(h.get(0).getEventType()).isEqualTo(HistoryEventType.QUESTION_ADDED);
        assertThat(h.get(0).getSummary()).isEqualTo("Open question added");
        assertThat(h.get(0).getAuthor().getLogin()).isEqualTo(EDITOR);
    }

    @Test
    void firstQuestionGetsSortOrderZero() throws Exception {
        mvc
            .perform(json(post("/api/tree/opportunities/{id}/questions", f.opportunity.getId()), "{\"text\":\"Sized?\"}", OWNER))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sortOrder").value(0));
    }

    @Test
    void tickEditAndRemoveWriteNoHistory() throws Exception {
        OpenQuestion q = persistQuestion("Old?", 0);
        mvc
            .perform(json(patch("/api/tree/questions/{id}", q.getId()), "{\"done\":true}", EDITOR))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.text").value("Old?"))
            .andExpect(jsonPath("$.done").value(true))
            .andExpect(jsonPath("$.sortOrder").value(0));
        mvc
            .perform(json(patch("/api/tree/questions/{id}", q.getId()), "{\"text\":\"New?\"}", OWNER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.text").value("New?"))
            .andExpect(jsonPath("$.done").value(true));
        mvc
            .perform(json(patch("/api/tree/questions/{id}", q.getId()), "{\"text\":\"New?\",\"done\":false}", OWNER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.done").value(false));
        mvc.perform(delete("/api/tree/questions/{id}", q.getId()).with(who(EDITOR)).with(csrf())).andExpect(status().isNoContent());

        assertThat(questionCount()).isZero();
        assertThat(history()).isEmpty();
    }

    @Test
    void invalidTextIs400() throws Exception {
        OpenQuestion q = persistQuestion("Keep?", 0);
        Long id = f.opportunity.getId();
        mvc
            .perform(json(post("/api/tree/opportunities/{id}/questions", id), "{\"text\":\"  \"}", OWNER))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.questiontextinvalid"));
        mvc
            .perform(json(post("/api/tree/opportunities/{id}/questions", id), "{}", OWNER))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.questiontextinvalid"));
        mvc
            .perform(json(post("/api/tree/opportunities/{id}/questions", id), "{\"text\":\"" + "q".repeat(501) + "\"}", OWNER))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.questiontextinvalid"));
        mvc
            .perform(json(patch("/api/tree/questions/{id}", q.getId()), "{\"text\":\"\"}", OWNER))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.questiontextinvalid"));
        assertThat(questionCount()).isEqualTo(1);
        assertThat(history()).isEmpty();
    }

    @Test
    void idsThatAreNotOpportunitiesAreForbidden() throws Exception {
        // Ids come from one shared sequence, so a solution's id is never an opportunity id:
        // it is treated like any unknown id (403, no existence leak).
        Long notAnOpportunity = f.solution.getId();
        mvc
            .perform(json(post("/api/tree/opportunities/{id}/questions", notAnOpportunity), "{\"text\":\"x\"}", OWNER))
            .andExpect(status().isForbidden());
    }

    @Test
    void viewerNonMemberAndAdminAreForbiddenAndNothingChanges() throws Exception {
        OpenQuestion q = persistQuestion("Keep?", 0);
        for (String login : List.of(VIEWER, OUTSIDER, "admin")) {
            mvc
                .perform(json(post("/api/tree/opportunities/{id}/questions", f.opportunity.getId()), "{\"text\":\"x\"}", login))
                .andExpect(status().isForbidden());
            mvc.perform(json(patch("/api/tree/questions/{id}", q.getId()), "{\"done\":true}", login)).andExpect(status().isForbidden());
            mvc.perform(delete("/api/tree/questions/{id}", q.getId()).with(who(login)).with(csrf())).andExpect(status().isForbidden());
        }
        mvc.perform(json(patch("/api/tree/questions/{id}", Long.MAX_VALUE), "{\"done\":true}", OWNER)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/tree/questions/{id}", Long.MAX_VALUE).with(who(OWNER)).with(csrf())).andExpect(status().isForbidden());

        em.clear();
        OpenQuestion reloaded = em.find(OpenQuestion.class, q.getId());
        assertThat(reloaded.getDone()).isFalse();
        assertThat(questionCount()).isEqualTo(1);
        assertThat(history()).isEmpty();
    }
}
