package com.opportunity.tree.service.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.MeetingTranscript;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.MeetingTranscriptSource;
import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.service.TeamAccessDeniedException;
import com.opportunity.tree.service.mcp.dto.TranscriptDetails;
import com.opportunity.tree.service.mcp.dto.TranscriptEntry;
import com.opportunity.tree.service.mcp.dto.TranscriptsPage;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

/**
 * MTRANS-007: integration tests for the {@code list_transcripts} and {@code get_transcript} MCP
 * tools. Covers the ticket's four acceptance criteria:
 * <ol>
 *   <li>list_transcripts supports team-wide and single-node queries, paginated metadata only,
 *       body never included.
 *   <li>get_transcript returns one full transcript only to team members; both tools enforce
 *       {@link com.opportunity.tree.service.TeamAccessService} and refuse cross-team / invalid ids.
 *   <li>(covered by the connect-agent spec — not in scope for this IT.)
 *   <li>MCP integration tests prove authorised results, metadata/body separation and cross-team
 *       denial.
 * </ol>
 */
@IntegrationTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TranscriptToolsIT {

    @Autowired
    private EntityManager em;

    @Autowired
    private ListTranscriptsTool listTool;

    @Autowired
    private GetTranscriptTool getTool;

    private Team teamA;
    private Team teamB;
    private User memberA;
    private User outsider;
    private Product productA;
    private Opportunity opportunityA1;
    private Opportunity opportunityA2;
    private Product productB;
    private Opportunity opportunityB;
    private MeetingTranscript t1;
    private MeetingTranscript t2;
    private MeetingTranscript t3;
    private MeetingTranscript otherTeamTranscript;

    @BeforeEach
    void seed() {
        String suffix = "-" + UUID.randomUUID().toString().substring(0, 8);
        teamA = persistTeam("teamA" + suffix);
        teamB = persistTeam("teamB" + suffix);
        memberA = persistUser("memberA" + suffix);
        outsider = persistUser("outsider" + suffix);
        persistMembership(teamA, memberA, TeamRole.OWNER);
        persistMembership(teamB, outsider, TeamRole.OWNER);

        productA = persistProduct(teamA, "Alpha");
        Outcome outcomeA = new Outcome().title("Grow WAU").sortOrder(0).createdDate(Instant.now()).product(productA);
        em.persist(outcomeA);
        opportunityA1 = persistOpportunity(outcomeA, "Onboarding is confusing", 0);
        opportunityA2 = persistOpportunity(outcomeA, "Empty state feels dead", 1);

        productB = persistProduct(teamB, "Beta");
        Outcome outcomeB = new Outcome().title("Retention").sortOrder(0).createdDate(Instant.now()).product(productB);
        em.persist(outcomeB);
        opportunityB = persistOpportunity(outcomeB, "Secret opportunity", 0);

        // Three transcripts on team A across two nodes; one on team B.
        t1 = persistTranscript(
            opportunityA1,
            memberA,
            "Kickoff",
            LocalDate.of(2026, 1, 1),
            Instant.parse("2026-01-01T09:00:00Z"),
            "opening notes A"
        );
        t2 = persistTranscript(
            opportunityA1,
            memberA,
            "Follow-up",
            LocalDate.of(2026, 1, 8),
            Instant.parse("2026-01-08T09:00:00Z"),
            "second call A"
        );
        t3 = persistTranscript(
            opportunityA2,
            memberA,
            "Empty state review",
            LocalDate.of(2026, 1, 15),
            Instant.parse("2026-01-15T09:00:00Z"),
            "empty state discussion"
        );
        otherTeamTranscript = persistTranscript(
            opportunityB,
            outsider,
            "Team B secret",
            LocalDate.of(2026, 1, 20),
            Instant.parse("2026-01-20T09:00:00Z"),
            "confidential team B body"
        );

        em.flush();
        em.clear();
    }

    // -------- Criterion 1: team-wide list returns metadata only, newest first --------

    @Test
    void teamWideListReturnsAllTranscriptsForTheTeamNewestFirstWithoutBody() {
        authenticate(memberA);
        TranscriptsPage page = listTool.listTranscripts(teamA.getId(), null, null, null, null);

        assertThat(page.teamId()).isEqualTo(teamA.getId());
        assertThat(page.nodeType()).isNull();
        assertThat(page.nodeId()).isNull();
        assertThat(page.totalMatching()).isEqualTo(3L);
        assertThat(page.transcripts()).extracting(TranscriptEntry::id).containsExactly(t3.getId(), t2.getId(), t1.getId());

        // Metadata/body separation — no field on the entry carries the body text.
        for (TranscriptEntry entry : page.transcripts()) {
            assertNoBodyField(entry);
        }
    }

    // -------- Criterion 1: single-node list is scoped to that node --------

    @Test
    void singleNodeListReturnsOnlyTheNodesTranscripts() {
        authenticate(memberA);
        TranscriptsPage page = listTool.listTranscripts(null, "OPPORTUNITY", opportunityA1.getId(), null, null);
        assertThat(page.nodeId()).isEqualTo(opportunityA1.getId());
        assertThat(page.nodeType()).isEqualTo("OPPORTUNITY");
        assertThat(page.totalMatching()).isEqualTo(2L);
        assertThat(page.transcripts()).extracting(TranscriptEntry::id).containsExactly(t2.getId(), t1.getId());
    }

    // -------- Criterion 1: pagination and cap --------

    @Test
    void teamWideListPaginates() {
        authenticate(memberA);
        TranscriptsPage first = listTool.listTranscripts(teamA.getId(), null, null, 0, 2);
        assertThat(first.transcripts()).hasSize(2);
        assertThat(first.totalMatching()).isEqualTo(3L);
        assertThat(first.page()).isEqualTo(0);
        assertThat(first.size()).isEqualTo(2);

        TranscriptsPage second = listTool.listTranscripts(teamA.getId(), null, null, 1, 2);
        assertThat(second.transcripts()).hasSize(1);
        assertThat(second.page()).isEqualTo(1);
    }

    @Test
    void pageSizeIsCappedAtMax() {
        authenticate(memberA);
        TranscriptsPage page = listTool.listTranscripts(teamA.getId(), null, null, 0, 5_000);
        assertThat(page.size()).isEqualTo(ListTranscriptsTool.MAX_PAGE_SIZE);
        assertThat(page.limit()).isEqualTo(ListTranscriptsTool.MAX_PAGE_SIZE);
    }

    // -------- Criterion 2: cross-team access is denied for both tools --------

    @Test
    void listTeamRefusesNonMember() {
        authenticate(outsider);
        assertThatThrownBy(() -> listTool.listTranscripts(teamA.getId(), null, null, null, null)).isInstanceOf(
            TeamAccessDeniedException.class
        );
    }

    @Test
    void listNodeRefusesNonMemberWithSameErrorAsMissingId() {
        authenticate(outsider);
        Throwable crossTeam = org.assertj.core.api.Assertions.catchThrowable(() ->
            listTool.listTranscripts(null, "OPPORTUNITY", opportunityA1.getId(), null, null)
        );
        Throwable missing = org.assertj.core.api.Assertions.catchThrowable(() ->
            listTool.listTranscripts(null, "OPPORTUNITY", 987_654_321L, null, null)
        );
        assertThat(crossTeam).isInstanceOf(TeamAccessDeniedException.class);
        assertThat(missing).isInstanceOf(TeamAccessDeniedException.class);
        assertThat(crossTeam.getMessage()).isEqualTo(missing.getMessage());
    }

    @Test
    void getTranscriptRefusesNonMember() {
        authenticate(outsider);
        assertThatThrownBy(() -> getTool.getTranscript(t1.getId())).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void getTranscriptRefusesUnknownIdWithSameError() {
        authenticate(memberA);
        Throwable missing = org.assertj.core.api.Assertions.catchThrowable(() -> getTool.getTranscript(987_654_321L));
        assertThat(missing).isInstanceOf(TeamAccessDeniedException.class);

        authenticate(outsider);
        Throwable crossTeam = org.assertj.core.api.Assertions.catchThrowable(() -> getTool.getTranscript(t1.getId()));
        assertThat(crossTeam).isInstanceOf(TeamAccessDeniedException.class);
        assertThat(crossTeam.getMessage()).isEqualTo(missing.getMessage());
    }

    // -------- Criterion 2: get_transcript returns full body to a member --------

    @Test
    void getTranscriptReturnsFullBodyToTeamMember() {
        authenticate(memberA);
        TranscriptDetails details = getTool.getTranscript(t1.getId());
        assertThat(details.id()).isEqualTo(t1.getId());
        assertThat(details.body()).isEqualTo("opening notes A");
        assertThat(details.title()).isEqualTo("Kickoff");
        assertThat(details.attendees()).isEqualTo("Alex, Sam");
        assertThat(details.source()).isEqualTo(MeetingTranscriptSource.PASTED);
        assertThat(details.nodeType()).isEqualTo(com.opportunity.tree.domain.enumeration.TreeNodeType.OPPORTUNITY);
        assertThat(details.nodeId()).isEqualTo(opportunityA1.getId());
        assertThat(details.authorLogin()).isEqualTo(memberA.getLogin());
    }

    // -------- Criterion 1 / 4: team scoping never leaks other-team rows --------

    @Test
    void teamAListDoesNotContainOtherTeamTranscripts() {
        authenticate(memberA);
        TranscriptsPage page = listTool.listTranscripts(teamA.getId(), null, null, null, null);
        assertThat(page.transcripts()).extracting(TranscriptEntry::id).doesNotContain(otherTeamTranscript.getId());
    }

    // -------- Argument validation --------

    @Test
    void bothScopesAtOnceIsRejected() {
        authenticate(memberA);
        assertThatThrownBy(() -> listTool.listTranscripts(teamA.getId(), "OPPORTUNITY", opportunityA1.getId(), null, null)).isInstanceOf(
            IllegalArgumentException.class
        );
    }

    @Test
    void neitherScopeIsRejected() {
        authenticate(memberA);
        assertThatThrownBy(() -> listTool.listTranscripts(null, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nodeIdAloneWithoutTypeIsRejected() {
        authenticate(memberA);
        assertThatThrownBy(() -> listTool.listTranscripts(null, null, opportunityA1.getId(), null, null)).isInstanceOf(
            IllegalArgumentException.class
        );
    }

    @Test
    void unknownNodeTypeIsRejectedBeforeAnyAccessCheck() {
        authenticate(memberA);
        assertThatThrownBy(() -> listTool.listTranscripts(null, "BOGUS", opportunityA1.getId(), null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("BOGUS");
    }

    // -------- Helpers --------

    private static void assertNoBodyField(TranscriptEntry entry) {
        for (Field f : TranscriptEntry.class.getDeclaredFields()) {
            assertThat(f.getName()).as("TranscriptEntry must not expose the transcript body").isNotEqualTo("body");
        }
        // Sanity: ensure every attribute we do expose is metadata (values populated).
        assertThat(entry.id()).isNotNull();
        assertThat(entry.title()).isNotBlank();
    }

    private Team persistTeam(String name) {
        Team t = new Team().name(name).description("d").createdDate(Instant.now());
        em.persist(t);
        return t;
    }

    private User persistUser(String login) {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setLogin(login);
        u.setActivated(true);
        u.setEmail(login + "@example.com");
        u.setFirstName(login);
        u.setLastName("test");
        u.setLangKey("en");
        em.persist(u);
        return u;
    }

    private void persistMembership(Team t, User u, TeamRole role) {
        TeamMember tm = new TeamMember().role(role).joinedDate(Instant.now());
        tm.setTeam(t);
        tm.setUser(u);
        em.persist(tm);
    }

    private Product persistProduct(Team team, String name) {
        Product p = new Product().name(name).description("d").archived(false).sortOrder(0).createdDate(Instant.now()).team(team);
        em.persist(p);
        return p;
    }

    private Opportunity persistOpportunity(Outcome outcome, String title, int sortOrder) {
        Opportunity o = new Opportunity()
            .title(title)
            .description("d")
            .status(OpportunityStatus.UNEXPLORED)
            .valuerating(4)
            .priority(50)
            .sortOrder(sortOrder)
            .createdDate(Instant.now())
            .outcome(outcome);
        em.persist(o);
        return o;
    }

    private MeetingTranscript persistTranscript(
        Opportunity opportunity,
        User author,
        String title,
        LocalDate meetingDate,
        Instant createdAt,
        String body
    ) {
        MeetingTranscript t = new MeetingTranscript()
            .title(title)
            .meetingDate(meetingDate)
            .attendees("Alex, Sam")
            .body(body)
            .source(MeetingTranscriptSource.PASTED)
            .createdDate(createdAt)
            .author(author)
            .opportunity(opportunity);
        em.persist(t);
        return t;
    }

    private void authenticate(User user) {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(user.getLogin(), "n/a"));
        SecurityContextHolder.setContext(ctx);
    }
}
