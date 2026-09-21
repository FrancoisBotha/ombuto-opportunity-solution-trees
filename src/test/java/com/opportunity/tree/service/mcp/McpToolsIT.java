package com.opportunity.tree.service.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.Interview;
import com.opportunity.tree.domain.NodeLink;
import com.opportunity.tree.domain.OpenQuestion;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.AssumptionStatus;
import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import com.opportunity.tree.domain.enumeration.SolutionStatus;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.service.TeamAccessDeniedException;
import com.opportunity.tree.service.mcp.dto.InterviewsPage;
import com.opportunity.tree.service.mcp.dto.NodeDetails;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * MCPSRV-004: end-to-end integration tests for the {@code get_node} and {@code list_interviews}
 * MCP tools.
 *
 * <p>Each test seeds two throwaway teams and switches the current authenticated user through the
 * Spring Security context. {@link com.opportunity.tree.security.SecurityUtils} then resolves the
 * caller from that context — the same code path the MCP bearer-token filter chain uses.
 */
@IntegrationTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class McpToolsIT {

    @Autowired
    private EntityManager em;

    @Autowired
    private GetNodeTool getNodeTool;

    @Autowired
    private ListInterviewsTool listInterviewsTool;

    @MockitoSpyBean
    private McpInterviewNotesPolicy notesPolicy;

    private Team teamA;
    private Team teamB;
    private User alice;
    private User bob;
    private User carol;
    private Product productA;
    private Product productB;
    private Outcome outcomeA;
    private Opportunity opportunityA;
    private Solution solutionA;
    private Assumption assumptionA;
    private Evidence evidenceA;
    private Interview interviewA1;
    private Interview interviewA2;

    @BeforeEach
    void seed() {
        String suffix = "-" + UUID.randomUUID().toString().substring(0, 8);
        teamA = persistTeam("teamA" + suffix);
        teamB = persistTeam("teamB" + suffix);
        alice = persistUser("alice" + suffix);
        bob = persistUser("bob" + suffix);
        carol = persistUser("carol" + suffix);
        persistMembership(teamA, alice, TeamRole.OWNER);
        persistMembership(teamA, carol, TeamRole.VIEWER);
        persistMembership(teamB, bob, TeamRole.OWNER);

        productA = persistProduct(teamA, "Alpha", 0);
        productB = persistProduct(teamB, "Bravo", 0);

        outcomeA = new Outcome().title("Grow WAU").description("desc").sortOrder(0).createdDate(Instant.now()).product(productA);
        em.persist(outcomeA);
        opportunityA = new Opportunity()
            .title("Onboarding is confusing")
            .description("odesc")
            .status(OpportunityStatus.UNEXPLORED)
            .valuerating(4)
            .priority(50)
            .sortOrder(0)
            .createdDate(Instant.now())
            .outcome(outcomeA);
        em.persist(opportunityA);
        solutionA = new Solution()
            .title("Interactive tour")
            .description("sdesc")
            .status(SolutionStatus.CANDIDATE)
            .sortOrder(0)
            .createdDate(Instant.now())
            .opportunity(opportunityA);
        em.persist(solutionA);
        assumptionA = new Assumption()
            .statement("Users will complete the tour")
            .description("adesc")
            .status(AssumptionStatus.UNTESTED)
            .confidence(40)
            .sortOrder(0)
            .createdDate(Instant.now())
            .solution(solutionA);
        em.persist(assumptionA);
        evidenceA = new Evidence().title("Interview snippet").sortOrder(0).createdDate(Instant.now()).opportunity(opportunityA);
        em.persist(evidenceA);

        interviewA1 = persistInterview(
            productA,
            "Kickoff call",
            "Ava Interviewee",
            LocalDate.of(2026, 1, 15),
            "sensitive notes 1",
            alice,
            Set.of(opportunityA)
        );
        interviewA2 = persistInterview(
            productA,
            "Follow-up",
            "Ben Interviewee",
            LocalDate.of(2026, 2, 20),
            "sensitive notes 2",
            alice,
            Set.of()
        );
        em.flush();
        em.clear();
    }

    // ---------------------------------------------------------------------
    // get_node
    // ---------------------------------------------------------------------

    @Test
    void getNode_inTeamMemberCanReadOpportunityDetails() {
        authenticate(alice);

        NodeDetails details = getNodeTool.getNode("OPPORTUNITY", opportunityA.getId());

        assertThat(details.type()).isEqualTo("OPPORTUNITY");
        assertThat(details.id()).isEqualTo(opportunityA.getId());
        assertThat(details.title()).isEqualTo("Onboarding is confusing");
        assertThat(details.description()).isEqualTo("odesc");
        assertThat(details.status()).isEqualTo("UNEXPLORED");
        assertThat(details.parent()).isNotNull();
        assertThat(details.parent().type()).isEqualTo("OUTCOME");
        assertThat(details.parent().id()).isEqualTo(outcomeA.getId());
        assertThat(details.parent().title()).isEqualTo("Grow WAU");
        assertThat(details.children()).extracting("type").contains("SOLUTION", "EVIDENCE");
        // AC #6: node with neither open questions nor links — both come back as empty lists
        // (opportunities always report both, even when empty; only inapplicable node types
        // return null, per AC #3 / the NodeDetails convention).
        assertThat(details.links()).isNotNull().isEmpty();
        assertThat(details.linkCount()).isEqualTo(0L);
        assertThat(details.openQuestions()).isNotNull().isEmpty();
        // Counts: opportunityA has 1 evidence child, no comments, no links.
        assertThat(details.evidenceCount()).isEqualTo(1L);
        assertThat(details.commentCount()).isEqualTo(0L);
    }

    @Test
    void getNode_opportunityReturnsOpenQuestionsAndLinksWhenBothPresent() {
        // Seed open questions and links directly, then read back through get_node. Covers AC
        // #1 (open questions with text and resolved state), #2 (links with target/type/title),
        // and the "node with both" case in AC #6.
        Instant now = Instant.now();
        Opportunity managed = em.find(Opportunity.class, opportunityA.getId());
        OpenQuestion q1 = new OpenQuestion()
            .questionText("How often do users retry onboarding?")
            .done(false)
            .sortOrder(0)
            .createdDate(now)
            .opportunity(managed);
        OpenQuestion q2 = new OpenQuestion()
            .questionText("Which step do they abandon on?")
            .done(true)
            .sortOrder(1)
            .createdDate(now)
            .opportunity(managed);
        em.persist(q1);
        em.persist(q2);

        NodeLink jira = new NodeLink().name("Jira Epic").url("https://ombuto.atlassian.net/browse/DISC-42").sortOrder(0).createdDate(now);
        jira.setOpportunity(managed);
        NodeLink confluence = new NodeLink()
            .name("Confluence")
            .url("https://ombuto.atlassian.net/wiki/discovery/opportunity-1")
            .sortOrder(1)
            .createdDate(now);
        confluence.setOpportunity(managed);
        NodeLink other = new NodeLink().name("Notion page").url("https://example.com/notes").sortOrder(2).createdDate(now);
        other.setOpportunity(managed);
        em.persist(jira);
        em.persist(confluence);
        em.persist(other);
        em.flush();
        em.clear();

        authenticate(alice);
        NodeDetails details = getNodeTool.getNode("OPPORTUNITY", opportunityA.getId());

        assertThat(details.openQuestions()).hasSize(2);
        assertThat(details.openQuestions().get(0).text()).isEqualTo("How often do users retry onboarding?");
        assertThat(details.openQuestions().get(0).resolved()).isFalse();
        assertThat(details.openQuestions().get(1).text()).isEqualTo("Which step do they abandon on?");
        assertThat(details.openQuestions().get(1).resolved()).isTrue();

        assertThat(details.links()).hasSize(3);
        assertThat(details.linkCount()).isEqualTo(3L);
        assertThat(details.links()).extracting("title").containsExactly("Jira Epic", "Confluence", "Notion page");
        assertThat(details.links())
            .extracting("target")
            .containsExactly(
                "https://ombuto.atlassian.net/browse/DISC-42",
                "https://ombuto.atlassian.net/wiki/discovery/opportunity-1",
                "https://example.com/notes"
            );
        assertThat(details.links()).extracting("type").containsExactly("JIRA", "CONFLUENCE", "OTHER");
    }

    @Test
    void getNode_openQuestionsIsNullForNonOpportunityNodeTypes() {
        // AC #3: fields that do not apply stay null, not fabricated as an empty list.
        authenticate(alice);
        assertThat(getNodeTool.getNode("PRODUCT", productA.getId()).openQuestions()).isNull();
        assertThat(getNodeTool.getNode("OUTCOME", outcomeA.getId()).openQuestions()).isNull();
        assertThat(getNodeTool.getNode("SOLUTION", solutionA.getId()).openQuestions()).isNull();
        assertThat(getNodeTool.getNode("ASSUMPTION", assumptionA.getId()).openQuestions()).isNull();
        assertThat(getNodeTool.getNode("EVIDENCE", evidenceA.getId()).openQuestions()).isNull();
    }

    @Test
    void getNode_nonMemberSeesNeitherOpenQuestionsNorLinks() {
        // AC #4 / #6 non-member row: even after we seed both, a caller who is not a member of
        // the owning team is refused at the TeamAccessService layer — the get_node call throws
        // instead of leaking any of the data.
        Instant now = Instant.now();
        Opportunity managed = em.find(Opportunity.class, opportunityA.getId());
        em.persist(new OpenQuestion().questionText("secret?").done(false).sortOrder(0).createdDate(now).opportunity(managed));
        NodeLink link = new NodeLink().name("Jira").url("https://ombuto.atlassian.net/browse/DISC-1").sortOrder(0).createdDate(now);
        link.setOpportunity(managed);
        em.persist(link);
        em.flush();
        em.clear();

        authenticate(bob); // bob is only in teamB
        assertThatThrownBy(() -> getNodeTool.getNode("OPPORTUNITY", opportunityA.getId())).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void getNode_productHasNoParentOrStatus() {
        authenticate(alice);

        NodeDetails details = getNodeTool.getNode("PRODUCT", productA.getId());

        assertThat(details.type()).isEqualTo("PRODUCT");
        assertThat(details.parent()).isNull();
        assertThat(details.status()).isNull();
        assertThat(details.evidenceCount()).isNull();
        assertThat(details.commentCount()).isNull();
        assertThat(details.openQuestions()).isNull();
        assertThat(details.links()).isNotNull().isEmpty();
        assertThat(details.linkCount()).isEqualTo(0L);
        assertThat(details.children()).extracting("id").contains(outcomeA.getId());
    }

    @Test
    void getNode_crossTeamIdIsRefusedWithAccessDenied() {
        authenticate(bob); // bob is only in teamB
        assertThatThrownBy(() -> getNodeTool.getNode("PRODUCT", productA.getId())).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void getNode_missingIdIsRefusedWithSameErrorShapeAsCrossTeam() {
        authenticate(bob);
        Throwable missing = org.assertj.core.api.Assertions.catchThrowable(() -> getNodeTool.getNode("PRODUCT", 987_654_321L));
        Throwable crossTeam = org.assertj.core.api.Assertions.catchThrowable(() -> getNodeTool.getNode("PRODUCT", productA.getId()));
        assertThat(missing).isInstanceOf(TeamAccessDeniedException.class);
        assertThat(crossTeam).isInstanceOf(TeamAccessDeniedException.class);
        assertThat(missing.getMessage()).isEqualTo(crossTeam.getMessage());
    }

    @Test
    void getNode_typeIdMismatchIsAClearValidationError() {
        authenticate(alice); // owns team A which contains opportunityA
        // opportunityA.getId() belongs to an OPPORTUNITY, not a SOLUTION; alice is a member.
        assertThatThrownBy(() -> getNodeTool.getNode("SOLUTION", opportunityA.getId()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("OPPORTUNITY")
            .hasMessageContaining("SOLUTION");
    }

    @Test
    void getNode_unknownTypeStringIsAValidationError() {
        authenticate(alice);
        assertThatThrownBy(() -> getNodeTool.getNode("BOGUS", opportunityA.getId()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("BOGUS");
    }

    // ---------------------------------------------------------------------
    // list_interviews
    // ---------------------------------------------------------------------

    @Test
    void listInterviews_byProduct_returnsOrderedInterviewsWithNotesForTeamMember() {
        authenticate(alice);

        InterviewsPage page = listInterviewsTool.listInterviews(productA.getId(), null, null, null);

        assertThat(page.teamId()).isEqualTo(teamA.getId());
        assertThat(page.productId()).isEqualTo(productA.getId());
        assertThat(page.notesIncluded()).isTrue();
        assertThat(page.interviews()).hasSize(2);
        assertThat(page.interviews().get(0).id()).isEqualTo(interviewA2.getId()); // later date first
        assertThat(page.interviews().get(0).notes()).isEqualTo("sensitive notes 2");
        assertThat(page.interviews().get(1).id()).isEqualTo(interviewA1.getId());
        assertThat(page.interviews().get(1).notes()).isEqualTo("sensitive notes 1");
        // Linked opportunities appear on the interview they are linked to.
        assertThat(page.interviews().get(1).opportunities()).extracting("id").contains(opportunityA.getId());
        assertThat(page.interviews().get(1).opportunities().get(0).status()).isEqualTo("UNEXPLORED");
        assertThat(page.limit()).isEqualTo(ListInterviewsTool.MAX_PAGE_SIZE);
        assertThat(page.totalMatching()).isEqualTo(2L);
    }

    @Test
    void listInterviews_byTeam_returnsInterviewsWithMetadata() {
        authenticate(alice);
        InterviewsPage page = listInterviewsTool.listInterviews(null, teamA.getId(), null, null);
        assertThat(page.interviews()).hasSize(2);
        assertThat(page.notesIncluded()).isTrue();
    }

    @Test
    void listInterviews_notesAreHiddenForRestrictedCaller() {
        authenticate(carol); // team-A viewer, but the notes policy is stubbed to refuse notes
        org.mockito.Mockito.doReturn(false).when(notesPolicy).canReadNotes(teamA.getId());

        InterviewsPage page = listInterviewsTool.listInterviews(productA.getId(), null, null, null);

        assertThat(page.notesIncluded()).isFalse();
        assertThat(page.interviews()).hasSize(2);
        assertThat(page.interviews()).extracting("title").contains("Kickoff call", "Follow-up");
        assertThat(page.interviews()).allMatch(s -> s.notes() == null);
        // The linked-opportunities metadata is still returned.
        assertThat(
            page
                .interviews()
                .stream()
                .flatMap(s -> s.opportunities().stream())
                .map(o -> o.id())
        ).contains(opportunityA.getId());
    }

    @Test
    void listInterviews_permittedCallerSeesNotes() {
        authenticate(carol); // team-A viewer
        // Do not stub the policy: the default rule permits team members.

        InterviewsPage page = listInterviewsTool.listInterviews(productA.getId(), null, null, null);

        assertThat(page.notesIncluded()).isTrue();
        assertThat(page.interviews()).allMatch(s -> s.notes() != null);
    }

    @Test
    void listInterviews_crossTeamProductIsRefused() {
        authenticate(bob);
        assertThatThrownBy(() -> listInterviewsTool.listInterviews(productA.getId(), null, null, null)).isInstanceOf(
            TeamAccessDeniedException.class
        );
    }

    @Test
    void listInterviews_crossTeamTeamIdIsRefused() {
        authenticate(bob);
        assertThatThrownBy(() -> listInterviewsTool.listInterviews(null, teamA.getId(), null, null)).isInstanceOf(
            TeamAccessDeniedException.class
        );
    }

    @Test
    void listInterviews_requiresProductOrTeam() {
        authenticate(alice);
        assertThatThrownBy(() -> listInterviewsTool.listInterviews(null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> listInterviewsTool.listInterviews(productA.getId(), teamA.getId(), null, null)).isInstanceOf(
            IllegalArgumentException.class
        );
    }

    @Test
    void listInterviews_capsPageSizeToMax() {
        authenticate(alice);
        InterviewsPage page = listInterviewsTool.listInterviews(productA.getId(), null, 0, 5000);
        assertThat(page.size()).isEqualTo(ListInterviewsTool.MAX_PAGE_SIZE);
        assertThat(page.limit()).isEqualTo(ListInterviewsTool.MAX_PAGE_SIZE);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

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

    private Product persistProduct(Team team, String name, int sortOrder) {
        Product p = new Product().name(name).description("d").archived(false).sortOrder(sortOrder).createdDate(Instant.now()).team(team);
        em.persist(p);
        return p;
    }

    private Interview persistInterview(
        Product product,
        String title,
        String participant,
        LocalDate date,
        String notes,
        User interviewer,
        Set<Opportunity> opportunities
    ) {
        Interview i = new Interview()
            .title(title)
            .participant(participant)
            .interviewDate(date)
            .notes(notes)
            .createdDate(Instant.now())
            .product(product)
            .interviewer(interviewer);
        em.persist(i);
        for (Opportunity op : opportunities) {
            // The many-to-many is owned by Opportunity.interviews (Interview.opportunities is
            // the mappedBy side), so link from the owning side to make Hibernate insert the row.
            op.getInterviews().add(i);
            i.getOpportunities().add(op);
        }
        return i;
    }

    private void authenticate(User user) {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(user.getLogin(), "n/a"));
        SecurityContextHolder.setContext(ctx);
    }
}
