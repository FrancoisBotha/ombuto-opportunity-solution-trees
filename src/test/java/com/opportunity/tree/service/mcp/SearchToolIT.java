package com.opportunity.tree.service.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Comment;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.Interview;
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
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.service.TeamAccessDeniedException;
import com.opportunity.tree.service.mcp.dto.SearchHit;
import com.opportunity.tree.service.mcp.dto.SearchPage;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration coverage for MCPSRV-013's {@link SearchTool}: real JPQL against H2, the real
 * {@code TeamAccessService}, and two teams seeded with distinct nodes so we can verify:
 *
 * <ul>
 *   <li>AC 5 — a cross-team search returns nothing (the query is bound to the caller's team
 *       ids at the DB layer, not filtered after the fact).</li>
 *   <li>AC 7 — team scoping: {@code teamId} narrows to one team; a {@code teamId} the caller
 *       is not a member of is refused with {@link TeamAccessDeniedException}, the same shape
 *       as an unknown id.</li>
 *   <li>AC 7 — pagination: offset / limit / hasMore work end-to-end.</li>
 *   <li>AC 6 — read-only: the tool never writes; both teams' data survives untouched.</li>
 * </ul>
 */
@IntegrationTest
@Transactional
class SearchToolIT {

    private static final String MEMBER_A_LOGIN = "search-member-a";
    private static final String MEMBER_B_LOGIN = "search-member-b";
    private static final String OUTSIDER_LOGIN = "search-outsider";

    @Autowired
    private EntityManager em;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SearchTool tool;

    @MockitoSpyBean
    private McpInterviewNotesPolicy notesPolicy;

    private Team teamA;
    private Team teamB;

    @BeforeEach
    void seed() {
        teamA = persistTeam("Alpha search");
        teamB = persistTeam("Bravo search");

        User memberA = persistUser(MEMBER_A_LOGIN);
        User memberB = persistUser(MEMBER_B_LOGIN);
        persistUser(OUTSIDER_LOGIN);

        persistMembership(teamA, memberA, TeamRole.OWNER);
        persistMembership(teamB, memberB, TeamRole.OWNER);

        // Team A: uses the term "checkout" in title and description across several types.
        Product pA = persistProduct(teamA, "Checkout revamp", "Rework the checkout flow", 0);
        Outcome oA = persistOutcome(pA, "Higher activation", "d", 0);
        Opportunity opA = persistOpportunity(oA, null, "Signup friction", "checkout users bounce", 0);
        Solution sA = persistSolution(opA, "One-tap signup", "d", 0);
        persistAssumption(sA, "Users trust one-tap", "d", 0);

        // Team B: does NOT contain the term "checkout" — the cross-team assertion.
        Product pB = persistProduct(teamB, "Onboarding", "Guide the first-run experience", 0);
        Outcome oB = persistOutcome(pB, "Reduce churn", "d", 0);
        persistOpportunity(oB, null, "Confusing empty state", "d", 0);

        em.flush();
        em.clear();
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------
    // AC 5: cross-team search returns nothing (never a redacted hit)
    // ---------------------------------------------------------------

    @Test
    void searchNodes_forMemberOfTeamB_neverSeesTeamAHits() {
        authenticate(MEMBER_B_LOGIN);

        SearchPage page = tool.searchNodes("checkout", null, null, null);

        // Team A has multiple "checkout" hits; team B has none. A member of team B must see
        // zero results, never a redacted hit, never a leaked team name.
        assertThat(page.total()).isZero();
        assertThat(page.hits()).isEmpty();
        assertThat(page.hits()).extracting(SearchHit::teamId).doesNotContain(teamA.getId());
    }

    @Test
    void searchNodes_forCallerInNoTeam_seesNoHitsAtAll() {
        authenticate(OUTSIDER_LOGIN);

        SearchPage page = tool.searchNodes("checkout", null, null, null);

        assertThat(page.total()).isZero();
        assertThat(page.hits()).isEmpty();
    }

    // ---------------------------------------------------------------
    // AC 7: team scoping — teamId narrows, cross-team teamId refused
    // ---------------------------------------------------------------

    @Test
    void searchNodes_withTeamIdNarrowsToThatTeam() {
        authenticate(MEMBER_A_LOGIN);

        SearchPage page = tool.searchNodes("checkout", teamA.getId(), null, null);

        assertThat(page.hits()).isNotEmpty();
        assertThat(page.hits()).extracting(SearchHit::teamId).containsOnly(teamA.getId());
    }

    @Test
    void searchNodes_withTeamIdTheCallerIsNotAMemberOf_isRefused() {
        authenticate(MEMBER_A_LOGIN);

        assertThatThrownBy(() -> tool.searchNodes("checkout", teamB.getId(), null, null)).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void searchNodes_withUnknownTeamId_isRefusedWithSameErrorShapeAsCrossTeam() {
        authenticate(MEMBER_A_LOGIN);

        Throwable crossTeam = catchThrowable(() -> tool.searchNodes("checkout", teamB.getId(), null, null));
        Throwable unknown = catchThrowable(() -> tool.searchNodes("checkout", 424_242L, null, null));

        assertThat(crossTeam).isInstanceOf(TeamAccessDeniedException.class);
        assertThat(unknown).isInstanceOf(TeamAccessDeniedException.class);
        assertThat(crossTeam.getMessage()).isEqualTo(unknown.getMessage());
    }

    // ---------------------------------------------------------------
    // AC 3 / AC 7: pagination
    // ---------------------------------------------------------------

    @Test
    void searchNodes_paginatesWithOffsetAndLimitAndReportsHasMore() {
        authenticate(MEMBER_A_LOGIN);
        // Seed enough matches to page. The @BeforeEach already added a few "checkout" hits in
        // team A; add more so we cross the small default page comfortably.
        Product pA = firstProductOfTeam(teamA);
        for (int i = 0; i < 30; i++) {
            Outcome extra = persistOutcome(pA, "checkout outcome " + i, "d", 10 + i);
            persistOpportunity(extra, null, "checkout opportunity " + i, "d", 10 + i);
        }
        em.flush();
        em.clear();

        SearchPage page1 = tool.searchNodes("checkout", null, 0, 10);
        SearchPage page2 = tool.searchNodes("checkout", null, 10, 10);

        assertThat(page1.hits()).hasSize(10);
        assertThat(page1.limit()).isEqualTo(10);
        assertThat(page1.offset()).isZero();
        assertThat(page1.hasMore()).isTrue();
        assertThat(page1.total()).isGreaterThanOrEqualTo(30L);
        assertThat(page2.offset()).isEqualTo(10);
        assertThat(page2.hits()).hasSize(10);
        // The three hits from the first page must not repeat on the second page.
        assertThat(page1.hits()).doesNotContainAnyElementsOf(page2.hits());
    }

    // ---------------------------------------------------------------
    // AC 1 / AC 6: read-only — get_tree carries descriptions now, and
    // search never mutates.
    // ---------------------------------------------------------------

    @Test
    void searchNodes_isReadOnly_teamRosterAndNodesUnchangedAfterCall() {
        authenticate(MEMBER_A_LOGIN);
        long teamsBefore = em.createQuery("select count(t) from Team t", Long.class).getSingleResult();
        long opportunitiesBefore = em.createQuery("select count(o) from Opportunity o", Long.class).getSingleResult();

        tool.searchNodes("checkout", null, null, null);

        long teamsAfter = em.createQuery("select count(t) from Team t", Long.class).getSingleResult();
        long opportunitiesAfter = em.createQuery("select count(o) from Opportunity o", Long.class).getSingleResult();
        assertThat(teamsAfter).isEqualTo(teamsBefore);
        assertThat(opportunitiesAfter).isEqualTo(opportunitiesBefore);
    }

    @Test
    void searchNodes_matchesDescriptionAndReturnsIt() {
        authenticate(MEMBER_A_LOGIN);
        // "users bounce" is only in a description, not in any title.
        SearchPage page = tool.searchNodes("users bounce", null, null, null);

        assertThat(page.hits()).extracting(SearchHit::title).contains("Signup friction");
        assertThat(page.hits())
            .extracting(SearchHit::description)
            .anyMatch(d -> d != null && d.contains("bounce"));
    }

    @Test
    void searchNodesFindsNotesCommentsAndQuestionsWithParentContext() {
        Opportunity op = em
            .createQuery("select o from Opportunity o where o.title = 'Signup friction'", Opportunity.class)
            .getSingleResult();
        User author = userRepository.findOneByLogin(MEMBER_A_LOGIN).orElseThrow();
        em.persist(new Comment().body("friction comment phrase").author(author).createdDate(Instant.now()).opportunity(op));
        Evidence evidence = new Evidence().title("Observation").sortOrder(0).createdDate(Instant.now()).opportunity(op);
        em.persist(evidence);
        em.persist(new Comment().body("friction evidence comment").author(author).createdDate(Instant.now()).evidence(evidence));
        em.persist(
            new OpenQuestion().questionText("friction question phrase?").done(false).sortOrder(0).createdDate(Instant.now()).opportunity(op)
        );
        Interview interview = new Interview()
            .title("Research call")
            .participant("Participant")
            .interviewDate(LocalDate.now())
            .notes("friction interview phrase")
            .createdDate(Instant.now())
            .product(op.getOutcome().getProduct());
        em.persist(interview);
        em.flush();
        em.clear();
        authenticate(MEMBER_A_LOGIN);

        SearchPage page = tool.searchNodes("friction", teamA.getId(), 0, 50);
        assertThat(page.hits()).extracting(SearchHit::type).contains("INTERVIEW_NOTE", "NODE_COMMENT", "OPEN_QUESTION");
        assertThat(
            page
                .hits()
                .stream()
                .filter(h -> h.type().equals("NODE_COMMENT") || h.type().equals("OPEN_QUESTION"))
        ).anyMatch(h -> h.parentType().equals("OPPORTUNITY") && h.parentId().equals(op.getId()));
        assertThat(
            page
                .hits()
                .stream()
                .filter(h -> h.type().equals("NODE_COMMENT"))
        ).anyMatch(h -> h.parentType().equals("EVIDENCE") && h.parentId().equals(evidence.getId()));
        assertThat(
            page
                .hits()
                .stream()
                .filter(h -> h.type().equals("INTERVIEW_NOTE"))
        ).allMatch(h -> h.parentType().equals("PRODUCT") && h.parentId().equals(op.getOutcome().getProduct().getId()));
    }

    @Test
    void searchNodesRestrictedNotesDoNotAffectHitsOrCounts() {
        Product product = firstProductOfTeam(teamA);
        em.persist(
            new Interview()
                .title("Private call")
                .participant("P")
                .interviewDate(LocalDate.now())
                .notes("uniquesecretnote")
                .createdDate(Instant.now())
                .product(product)
        );
        em.flush();
        em.clear();
        authenticate(MEMBER_A_LOGIN);
        org.mockito.Mockito.doReturn(false).when(notesPolicy).canReadNotes(teamA.getId());

        SearchPage page = tool.searchNodes("uniquesecretnote", teamA.getId(), 0, 20);
        assertThat(page.total()).isZero();
        assertThat(page.hits()).isEmpty();
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static Throwable catchThrowable(Runnable r) {
        try {
            r.run();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    private Product firstProductOfTeam(Team t) {
        return em
            .createQuery("select p from Product p where p.team.id = :id order by p.id", Product.class)
            .setParameter("id", t.getId())
            .getResultList()
            .get(0);
    }

    private Team persistTeam(String name) {
        Team t = new Team().name(name).description("d").createdDate(Instant.now());
        em.persist(t);
        em.flush();
        return t;
    }

    private User persistUser(String login) {
        return userRepository
            .findOneByLogin(login)
            .orElseGet(() -> {
                User u = new User();
                u.setId(UUID.randomUUID().toString());
                u.setLogin(login);
                u.setActivated(true);
                u.setEmail(login + "@example.com");
                u.setFirstName(login);
                u.setLastName("test");
                u.setLangKey("en");
                em.persist(u);
                em.flush();
                return u;
            });
    }

    private void persistMembership(Team t, User u, TeamRole role) {
        TeamMember tm = new TeamMember().role(role).joinedDate(Instant.now());
        tm.setTeam(t);
        tm.setUser(u);
        em.persist(tm);
        em.flush();
    }

    private Product persistProduct(Team team, String name, String description, int sortOrder) {
        Product p = new Product()
            .name(name)
            .description(description)
            .archived(false)
            .sortOrder(sortOrder)
            .createdDate(Instant.now())
            .team(team);
        em.persist(p);
        return p;
    }

    private Outcome persistOutcome(Product product, String title, String description, int sortOrder) {
        Outcome o = new Outcome().title(title).description(description).sortOrder(sortOrder).createdDate(Instant.now()).product(product);
        em.persist(o);
        return o;
    }

    private Opportunity persistOpportunity(Outcome outcome, Opportunity parent, String title, String description, int sortOrder) {
        Opportunity op = new Opportunity()
            .title(title)
            .description(description)
            .status(OpportunityStatus.UNEXPLORED)
            .valuerating(3)
            .priority(50)
            .sortOrder(sortOrder)
            .createdDate(Instant.now())
            .outcome(outcome)
            .parent(parent);
        em.persist(op);
        return op;
    }

    private Solution persistSolution(Opportunity opportunity, String title, String description, int sortOrder) {
        Solution s = new Solution()
            .title(title)
            .description(description)
            .status(SolutionStatus.CANDIDATE)
            .sortOrder(sortOrder)
            .createdDate(Instant.now())
            .opportunity(opportunity);
        em.persist(s);
        return s;
    }

    private Assumption persistAssumption(Solution solution, String statement, String description, int sortOrder) {
        Assumption a = new Assumption()
            .statement(statement)
            .description(description)
            .status(AssumptionStatus.UNTESTED)
            .confidence(1)
            .sortOrder(sortOrder)
            .createdDate(Instant.now())
            .solution(solution);
        em.persist(a);
        return a;
    }

    private static void authenticate(String login) {
        var auth = new UsernamePasswordAuthenticationToken(login, "n/a", List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
