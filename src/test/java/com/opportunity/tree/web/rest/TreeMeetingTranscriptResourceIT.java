package com.opportunity.tree.web.rest;

import static com.opportunity.tree.web.rest.TreeCollaborationFixture.EDITOR;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.OUTSIDER;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.OWNER;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.VIEWER;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.who;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.MeetingTranscript;
import com.opportunity.tree.domain.NodeHistory;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.MeetingTranscriptSource;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.service.TreeMeetingTranscriptService;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link TreeMeetingTranscriptResource}: team-scoped meeting transcript
 * CRUD (Epic 12 / MTRANS-002). Covers acceptance criteria 1–6: paginated metadata lists newest
 * first, body-only single read, TeamAccessService gating, exactly-one node + cross-team rejection,
 * server-set author/createdDate, immutable node, 1 MiB body ceiling, blank-body rejection,
 * TRANSCRIPT_ADDED / TRANSCRIPT_DELETED history without body text in summaries.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class TreeMeetingTranscriptResourceIT {

    private static final String NODE_TRANSCRIPTS = "/api/tree/nodes/{type}/{id}/transcripts";
    private static final String TEAM_TRANSCRIPTS = "/api/tree/teams/{teamId}/transcripts";
    private static final String TRANSCRIPT = "/api/tree/transcripts/{id}";
    private static final String CREATE = "/api/tree/transcripts";

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

    private MeetingTranscript persistTranscript(String title, LocalDate date, Instant createdAt, String body) {
        MeetingTranscript t = new MeetingTranscript()
            .title(title)
            .meetingDate(date)
            .body(body)
            .source(MeetingTranscriptSource.PASTED)
            .createdDate(createdAt)
            .author(f.owner)
            .opportunity(f.opportunity);
        em.persist(t);
        em.flush();
        return t;
    }

    private String createBody(Long opportunityId, String title, String body) {
        return (
            "{\"title\":\"" +
            title +
            "\",\"meetingDate\":\"2026-01-15\"," +
            "\"attendees\":\"Alex, Sam\",\"body\":\"" +
            body +
            "\"," +
            "\"opportunityId\":" +
            opportunityId +
            "}"
        );
    }

    // --- Criterion 1: paginated metadata lists newest first, omit body ---

    @Test
    void listByNodeReturnsPagedMetadataNewestFirstAndOmitsBody() throws Exception {
        persistTranscript("first", LocalDate.of(2026, 1, 1), Instant.parse("2026-01-01T10:00:00Z"), "aaa");
        persistTranscript("second", LocalDate.of(2026, 1, 2), Instant.parse("2026-01-02T10:00:00Z"), "bbb");
        persistTranscript("third", LocalDate.of(2026, 1, 3), Instant.parse("2026-01-03T10:00:00Z"), "ccc");

        mvc
            .perform(get(NODE_TRANSCRIPTS, "opportunity", f.opportunity.getId()).with(who(VIEWER)))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "3"))
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].title").value("third"))
            .andExpect(jsonPath("$[1].title").value("second"))
            .andExpect(jsonPath("$[2].title").value("first"))
            .andExpect(jsonPath("$[0].body").doesNotExist())
            .andExpect(jsonPath("$[0].nodeType").value("OPPORTUNITY"))
            .andExpect(jsonPath("$[0].nodeKey").value("opportunity-" + f.opportunity.getId()));
    }

    @Test
    void listByTeamReturnsPagedMetadataAndOmitsBody() throws Exception {
        persistTranscript("t1", LocalDate.of(2026, 1, 1), Instant.parse("2026-01-01T10:00:00Z"), "aaa");
        MeetingTranscript onEvidence = new MeetingTranscript()
            .title("on-evidence")
            .meetingDate(LocalDate.of(2026, 1, 5))
            .body("zzz")
            .source(MeetingTranscriptSource.PASTED)
            .createdDate(Instant.parse("2026-01-05T10:00:00Z"))
            .author(f.owner)
            .evidence(f.evidence);
        em.persist(onEvidence);
        em.flush();

        mvc
            .perform(get(TEAM_TRANSCRIPTS, f.team.getId()).with(who(VIEWER)))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "2"))
            .andExpect(jsonPath("$[0].title").value("on-evidence"))
            .andExpect(jsonPath("$[0].body").doesNotExist())
            .andExpect(jsonPath("$[1].title").value("t1"));
    }

    // --- Criterion 2: body only on single read; access gated by TeamAccessService ---

    @Test
    void singleReadReturnsBody() throws Exception {
        MeetingTranscript t = persistTranscript("v1", LocalDate.of(2026, 1, 1), Instant.now(), "the whole body");

        mvc
            .perform(get(TRANSCRIPT, t.getId()).with(who(VIEWER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.body").value("the whole body"))
            .andExpect(jsonPath("$.title").value("v1"))
            .andExpect(jsonPath("$.nodeType").value("OPPORTUNITY"));
    }

    @Test
    void listAndReadRejectNonMemberAndAdmin() throws Exception {
        MeetingTranscript t = persistTranscript("secret", LocalDate.of(2026, 1, 1), Instant.now(), "shh");
        for (String login : List.of(OUTSIDER, "admin")) {
            mvc.perform(get(NODE_TRANSCRIPTS, "opportunity", f.opportunity.getId()).with(who(login))).andExpect(status().isForbidden());
            mvc.perform(get(TRANSCRIPT, t.getId()).with(who(login))).andExpect(status().isForbidden());
            mvc.perform(get(TEAM_TRANSCRIPTS, f.team.getId()).with(who(login))).andExpect(status().isForbidden());
        }
    }

    // --- Criterion 3: exactly one node; cross-team rejection; viewers cannot write ---

    @Test
    void createRequiresExactlyOneNodeRelationship() throws Exception {
        mvc
            .perform(json(post(CREATE), "{\"title\":\"t\",\"meetingDate\":\"2026-01-15\",\"attendees\":null,\"body\":\"hi\"}", EDITOR))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.transcriptnodeinvalid"));
        // Two node ids at once → also invalid.
        mvc
            .perform(
                json(
                    post(CREATE),
                    "{\"title\":\"t\",\"meetingDate\":\"2026-01-15\",\"body\":\"hi\"," +
                        "\"opportunityId\":" +
                        f.opportunity.getId() +
                        ",\"solutionId\":" +
                        f.solution.getId() +
                        "}",
                    EDITOR
                )
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.transcriptnodeinvalid"));
        assertThat(f.count("select count(t) from MeetingTranscript t")).isZero();
    }

    @Test
    void createRejectsCrossTeamNodeAttachment() throws Exception {
        // Product in another team; f.editor is not a member of otherTeam.
        Product outsiderProduct = new Product().name("other").archived(false).sortOrder(0).createdDate(Instant.now()).team(f.otherTeam);
        em.persist(outsiderProduct);
        em.flush();
        mvc
            .perform(
                json(
                    post(CREATE),
                    "{\"title\":\"t\",\"meetingDate\":\"2026-01-15\",\"body\":\"hi\"," + "\"productId\":" + outsiderProduct.getId() + "}",
                    EDITOR
                )
            )
            .andExpect(status().isForbidden());
        assertThat(f.count("select count(t) from MeetingTranscript t")).isZero();
    }

    @Test
    void viewerCannotCreateEditOrDeleteButCanListAndRead() throws Exception {
        MeetingTranscript t = persistTranscript("viewer-visible", LocalDate.of(2026, 1, 1), Instant.now(), "body");

        mvc.perform(get(NODE_TRANSCRIPTS, "opportunity", f.opportunity.getId()).with(who(VIEWER))).andExpect(status().isOk());
        mvc.perform(get(TRANSCRIPT, t.getId()).with(who(VIEWER))).andExpect(status().isOk());
        mvc.perform(json(post(CREATE), createBody(f.opportunity.getId(), "new", "hello"), VIEWER)).andExpect(status().isForbidden());
        mvc
            .perform(json(patch(TRANSCRIPT, t.getId()), "{\"title\":\"edit\",\"meetingDate\":\"2026-01-01\",\"body\":\"x\"}", VIEWER))
            .andExpect(status().isForbidden());
        mvc.perform(delete(TRANSCRIPT, t.getId()).with(who(VIEWER)).with(csrf())).andExpect(status().isForbidden());
    }

    // --- Criterion 4: server-set author/createdDate, editedDate on update, node immutable, delete leaves node ---

    @Test
    void editorCreateSetsAuthorAndCreatedDateAndWritesHistory() throws Exception {
        Instant beforeCreate = Instant.now().minusSeconds(1);
        mvc
            .perform(json(post(CREATE), createBody(f.opportunity.getId(), "Discovery call", "line1\\nline2"), EDITOR))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.title").value("Discovery call"))
            .andExpect(jsonPath("$.body").value("line1\nline2"))
            .andExpect(jsonPath("$.authorLogin").value(EDITOR))
            .andExpect(jsonPath("$.authorInitials").value("AR"))
            .andExpect(jsonPath("$.editedDate").doesNotExist())
            .andExpect(jsonPath("$.createdDate").isNotEmpty());
        List<MeetingTranscript> rows = em.createQuery("select t from MeetingTranscript t", MeetingTranscript.class).getResultList();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getAuthor().getLogin()).isEqualTo(EDITOR);
        assertThat(rows.get(0).getCreatedDate()).isAfter(beforeCreate);

        List<NodeHistory> h = history(TreeNodeType.OPPORTUNITY, f.opportunity.getId());
        assertThat(h).hasSize(1);
        assertThat(h.get(0).getEventType()).isEqualTo(HistoryEventType.TRANSCRIPT_ADDED);
        assertThat(h.get(0).getSummary()).isEqualTo("Transcript added");
        // Body must never leak into the summary.
        assertThat(h.get(0).getSummary()).doesNotContain("line1").doesNotContain("line2");
    }

    @Test
    void updateStampsEditedDateAndCannotMoveTranscriptToAnotherNode() throws Exception {
        MeetingTranscript t = persistTranscript("orig", LocalDate.of(2026, 1, 1), Instant.now(), "orig body");
        Long originalOpportunityId = f.opportunity.getId();
        // Send solutionId in an attempted move — service must ignore node fields on update.
        mvc
            .perform(
                json(
                    patch(TRANSCRIPT, t.getId()),
                    "{\"title\":\"edited\",\"meetingDate\":\"2026-01-02\",\"body\":\"new body\"," +
                        "\"solutionId\":" +
                        f.solution.getId() +
                        "}",
                    EDITOR
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("edited"))
            .andExpect(jsonPath("$.body").value("new body"))
            .andExpect(jsonPath("$.editedDate").isNotEmpty())
            .andExpect(jsonPath("$.nodeType").value("OPPORTUNITY"))
            .andExpect(jsonPath("$.nodeId").value(originalOpportunityId));
        em.clear();
        MeetingTranscript reloaded = em.find(MeetingTranscript.class, t.getId());
        assertThat(reloaded.getOpportunity().getId()).isEqualTo(originalOpportunityId);
        assertThat(reloaded.getSolution()).isNull();
        assertThat(reloaded.getEditedDate()).isNotNull();
    }

    @Test
    void deleteRemovesTranscriptButLeavesTheNodeIntact() throws Exception {
        MeetingTranscript t = persistTranscript("bye", LocalDate.of(2026, 1, 1), Instant.now(), "body");
        Long nodeId = f.opportunity.getId();

        mvc.perform(delete(TRANSCRIPT, t.getId()).with(who(EDITOR)).with(csrf())).andExpect(status().isNoContent());

        assertThat(f.count("select count(t) from MeetingTranscript t where t.id = ?1", t.getId())).isZero();
        assertThat(f.count("select count(o) from Opportunity o where o.id = ?1", nodeId)).isEqualTo(1);
        List<NodeHistory> h = history(TreeNodeType.OPPORTUNITY, nodeId);
        assertThat(h).hasSize(1);
        assertThat(h.get(0).getEventType()).isEqualTo(HistoryEventType.TRANSCRIPT_DELETED);
        assertThat(h.get(0).getSummary()).isEqualTo("Transcript deleted");
    }

    // --- Criterion 5: 1 MiB UTF-8 ceiling, blank body rejected; body never in logs/websockets ---

    @Test
    void blankBodyIsRejected() throws Exception {
        mvc
            .perform(json(post(CREATE), createBody(f.opportunity.getId(), "hi", "   "), EDITOR))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.transcriptbodyinvalid"));
    }

    @Test
    void bodyLargerThanOneMibIsRejected() throws Exception {
        String body = "x".repeat(TreeMeetingTranscriptService.BODY_MAX_BYTES + 1);
        String payload =
            "{\"title\":\"big\",\"meetingDate\":\"2026-01-15\",\"body\":\"" +
            body +
            "\"," +
            "\"opportunityId\":" +
            f.opportunity.getId() +
            "}";
        mvc
            .perform(json(post(CREATE), payload, EDITOR))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.transcriptbodytoolarge"));
    }

    @Test
    void entityToStringDoesNotContainBody() {
        MeetingTranscript t = persistTranscript(
            "privacy-check",
            LocalDate.of(2026, 1, 1),
            Instant.now(),
            "SECRET-CUSTOMER-QUOTE that must not appear in a log"
        );
        // The service and resource use SLF4J logging that calls toString() on their arguments;
        // MeetingTranscript.toString() must not embed the body verbatim (NFR-022 / criterion 5).
        assertThat(t.toString()).doesNotContain("SECRET-CUSTOMER-QUOTE");
    }

    // --- Criterion 6: history entries for TRANSCRIPT_ADDED / TRANSCRIPT_DELETED with no body ---

    @Test
    void addAndDeleteWriteHistoryWithoutBodyText() throws Exception {
        mvc
            .perform(json(post(CREATE), createBody(f.opportunity.getId(), "H1", "confidential CUSTOMER answer"), EDITOR))
            .andExpect(status().isCreated())
            .andReturn();
        Long id = em.createQuery("select t.id from MeetingTranscript t", Long.class).getSingleResult();

        List<NodeHistory> afterAdd = history(TreeNodeType.OPPORTUNITY, f.opportunity.getId());
        assertThat(afterAdd).hasSize(1);
        assertThat(afterAdd.get(0).getSummary()).doesNotContain("CUSTOMER");

        mvc.perform(delete(TRANSCRIPT, id).with(who(EDITOR)).with(csrf())).andExpect(status().isNoContent());
        List<NodeHistory> afterDelete = history(TreeNodeType.OPPORTUNITY, f.opportunity.getId());
        assertThat(afterDelete)
            .extracting(NodeHistory::getEventType)
            .containsExactly(HistoryEventType.TRANSCRIPT_ADDED, HistoryEventType.TRANSCRIPT_DELETED);
        for (NodeHistory h : afterDelete) {
            assertThat(h.getSummary()).doesNotContain("CUSTOMER").doesNotContain("confidential");
        }
    }

    @Test
    void unknownIdsAndCrossTeamReadsGet403() throws Exception {
        mvc.perform(get(TRANSCRIPT, Long.MAX_VALUE).with(who(OWNER))).andExpect(status().isForbidden());
        mvc.perform(delete(TRANSCRIPT, Long.MAX_VALUE).with(who(OWNER)).with(csrf())).andExpect(status().isForbidden());
        mvc
            .perform(json(patch(TRANSCRIPT, Long.MAX_VALUE), "{\"title\":\"x\",\"meetingDate\":\"2026-01-01\",\"body\":\"y\"}", OWNER))
            .andExpect(status().isForbidden());
    }
}
