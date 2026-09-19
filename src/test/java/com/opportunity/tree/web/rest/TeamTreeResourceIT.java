package com.opportunity.tree.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Comment;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.NodeHistory;
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
import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import com.opportunity.tree.domain.enumeration.SolutionStatus;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.service.TeamAccessDeniedException;
import com.opportunity.tree.service.TeamTreeService;
import com.opportunity.tree.service.dto.tree.TeamTreeDTO;
import com.opportunity.tree.service.dto.tree.TreeNodeDTO;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link TeamTreeResource} / {@link TeamTreeService}: the
 * flat, pre-ordered whole-team tree read. Every test runs in a transaction that
 * is rolled back, so no explicit cleanup is needed.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class TeamTreeResourceIT {

    private static final String OWNER_LOGIN = "tree-owner";
    private static final String EDITOR_LOGIN = "tree-editor";
    private static final String VIEWER_LOGIN = "tree-viewer";
    private static final String OUTSIDER_LOGIN = "tree-outsider";

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamTreeService teamTreeService;

    private Team teamA;
    private Team teamB;
    private User ownerUser;
    private User editorUser;

    @BeforeEach
    void setUp() {
        teamA = persistTeam("Tree Team A");
        teamB = persistTeam("Tree Team B");

        ownerUser = persistUser(OWNER_LOGIN, "Olive", "Owner");
        editorUser = persistUser(EDITOR_LOGIN, "Eddie", "Editor");
        User viewerUser = persistUser(VIEWER_LOGIN, null, null);
        persistUser(OUTSIDER_LOGIN, "Out", "Sider");

        persistMembership(teamA, ownerUser, TeamRole.OWNER);
        persistMembership(teamA, editorUser, TeamRole.EDITOR);
        persistMembership(teamA, viewerUser, TeamRole.VIEWER);
        persistMembership(teamB, ownerUser, TeamRole.OWNER);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------
    // Fixture (team A):
    //
    //   productA2 (sortOrder 1, archived)
    //   productA1 (sortOrder 2)            <- link, lastActivity from asm
    //     outcome1                          <- 1 comment
    //       oppTop2 (sortOrder 0)
    //       oppTop  (sortOrder 1)           <- 2 comments, 2 links, 2 questions
    //         childOpp (sortOrder 5)        <- nested opportunity first
    //         sol      (sortOrder 1)
    //           asm                          <- 1 comment, owner = editor
    //             evAsm (created 40 days ago)
    //         evOpp    (sortOrder 0)        <- evidence last despite sortOrder
    //
    // Team B holds a product/outcome/opportunity/evidence that must not leak.
    // ---------------------------------------------------------------
    private static class Fixture {

        Product productA1;
        Product productA2;
        Outcome outcome1;
        Opportunity oppTop;
        Opportunity oppTop2;
        Opportunity childOpp;
        Solution sol;
        Assumption asm;
        Evidence evAsm;
        Evidence evOpp;
        NodeLink linkFirst;
        NodeLink linkSecond;
        OpenQuestion q1;
        OpenQuestion q2;
        Instant newestHistoryAt;
        Product productB;
    }

    private Fixture seedFixture() {
        Instant now = Instant.now();
        Fixture f = new Fixture();
        f.productA1 = persistProduct(teamA, "A1", false, 2);
        f.productA2 = persistProduct(teamA, "A2", true, 1);
        f.outcome1 = persistOutcome(f.productA1, "outcome-1", 1);
        f.oppTop = persistOpportunity(f.outcome1, null, "opp-top", 1);
        f.oppTop.setPriority(70);
        f.oppTop.setValuerating(4);
        f.oppTop.setStatus(OpportunityStatus.VALIDATED);
        f.oppTop.setDescription("notes on opp");
        f.oppTop2 = persistOpportunity(f.outcome1, null, "opp-top-2", 0);
        f.childOpp = persistOpportunity(f.outcome1, f.oppTop, "child-opp", 5);
        f.sol = persistSolution(f.oppTop, "sol", 1);
        f.asm = new Assumption()
            .statement("people want it")
            .status(AssumptionStatus.TESTING)
            .confidence(70)
            .sortOrder(0)
            .createdDate(now)
            .solution(f.sol)
            .owner(editorUser);
        em.persist(f.asm);
        f.evAsm = new Evidence().title("old test result").sortOrder(0).createdDate(now.minus(Duration.ofDays(40))).assumption(f.asm);
        em.persist(f.evAsm);
        f.evOpp = new Evidence().title("interview snippet").sortOrder(0).createdDate(now).opportunity(f.oppTop);
        em.persist(f.evOpp);

        persistComment(ownerUser).opportunity(f.oppTop);
        persistComment(editorUser).opportunity(f.oppTop);
        persistComment(ownerUser).assumption(f.asm);
        persistComment(ownerUser).outcome(f.outcome1);

        f.linkSecond = new NodeLink().name("Spec").url("https://example.com/spec").sortOrder(1).createdDate(now).opportunity(f.oppTop);
        f.linkFirst = new NodeLink().name("Board").url("https://example.com/board").sortOrder(0).createdDate(now).opportunity(f.oppTop);
        em.persist(f.linkSecond);
        em.persist(f.linkFirst);
        em.persist(new NodeLink().name("Home").url("https://example.com/").sortOrder(0).createdDate(now).product(f.productA1));

        f.q2 = new OpenQuestion().questionText("Second?").done(true).sortOrder(1).createdDate(now).opportunity(f.oppTop);
        f.q1 = new OpenQuestion().questionText("First?").done(false).sortOrder(0).createdDate(now).opportunity(f.oppTop);
        em.persist(f.q2);
        em.persist(f.q1);

        em.flush(); // ids are needed for the history rows
        f.newestHistoryAt = now.minus(Duration.ofHours(1));
        persistHistory(TreeNodeType.SOLUTION, f.sol.getId(), now.minus(Duration.ofHours(3)), editorUser);
        persistHistory(TreeNodeType.ASSUMPTION, f.asm.getId(), now.minus(Duration.ofHours(2)), editorUser);
        persistHistory(TreeNodeType.ASSUMPTION, f.asm.getId(), f.newestHistoryAt, ownerUser);
        persistHistory(TreeNodeType.OUTCOME, f.outcome1.getId(), now.minus(Duration.ofDays(3)), editorUser);

        // Team B — must never appear; its history is newer than anything in team A.
        f.productB = persistProduct(teamB, "B1", false, 0);
        Outcome outcomeB = persistOutcome(f.productB, "b-outcome", 0);
        Opportunity oppB = persistOpportunity(outcomeB, null, "b-opp", 0);
        em.persist(new Evidence().title("b-evidence").sortOrder(0).createdDate(now).opportunity(oppB));
        em.flush();
        persistHistory(TreeNodeType.OPPORTUNITY, oppB.getId(), now, ownerUser);

        em.flush();
        em.clear();
        return f;
    }

    // ---------------------------------------------------------------
    // Authorisation
    // ---------------------------------------------------------------

    @Test
    void unauthenticatedCallerGets401() throws Exception {
        mvc.perform(get("/api/teams/{teamId}/tree", teamA.getId())).andExpect(status().isUnauthorized());
    }

    @Test
    void ownerAndEditorCanEdit() throws Exception {
        seedFixture();
        mvc
            .perform(get("/api/teams/{teamId}/tree", teamA.getId()).with(user(OWNER_LOGIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentUserLogin").value(OWNER_LOGIN))
            .andExpect(jsonPath("$.currentUserRole").value("OWNER"))
            .andExpect(jsonPath("$.canEdit").value(true));
        mvc
            .perform(get("/api/teams/{teamId}/tree", teamA.getId()).with(user(EDITOR_LOGIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentUserRole").value("EDITOR"))
            .andExpect(jsonPath("$.canEdit").value(true));
    }

    @Test
    void viewerCanReadWholeTreeButCannotEdit() throws Exception {
        seedFixture();
        mvc
            .perform(get("/api/teams/{teamId}/tree", teamA.getId()).with(user(VIEWER_LOGIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentUserLogin").value(VIEWER_LOGIN))
            .andExpect(jsonPath("$.currentUserRole").value("VIEWER"))
            .andExpect(jsonPath("$.canEdit").value(false))
            .andExpect(jsonPath("$.nodes", hasSize(10)));
    }

    @Test
    void nonMemberGets403() throws Exception {
        seedFixture();
        mvc.perform(get("/api/teams/{teamId}/tree", teamA.getId()).with(user(OUTSIDER_LOGIN))).andExpect(status().isForbidden());
    }

    @Test
    void adminWhoIsNotAMemberGets403() throws Exception {
        seedFixture();
        mvc
            .perform(get("/api/teams/{teamId}/tree", teamA.getId()).with(user(OUTSIDER_LOGIN).roles("ADMIN", "USER")))
            .andExpect(status().isForbidden());
    }

    @Test
    void unknownTeamGets403LikeANonMemberTeam() throws Exception {
        mvc.perform(get("/api/teams/{teamId}/tree", Long.MAX_VALUE).with(user(OWNER_LOGIN))).andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------
    // JSON shape
    // ---------------------------------------------------------------

    @Test
    void jsonShapeExposesFlatNodesMembersAndCounters() throws Exception {
        Fixture f = seedFixture();
        String oppKey = "opportunity-" + f.oppTop.getId();
        mvc
            .perform(get("/api/teams/{teamId}/tree", teamA.getId()).with(user(EDITOR_LOGIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(teamA.getId()))
            .andExpect(jsonPath("$.name").value("Tree Team A"))
            .andExpect(jsonPath("$.evidenceThisMonth").value(1))
            .andExpect(jsonPath("$.members[*].login", contains(OWNER_LOGIN, EDITOR_LOGIN, VIEWER_LOGIN)))
            .andExpect(jsonPath("$.members[0].firstName").value("Olive"))
            .andExpect(jsonPath("$.members[0].lastName").value("Owner"))
            .andExpect(jsonPath("$.members[0].initials").value("OO"))
            .andExpect(jsonPath("$.members[0].role").value("OWNER"))
            .andExpect(jsonPath("$.members[2].initials").value("TR"))
            .andExpect(jsonPath("$.nodes[0].key").value("product-" + f.productA2.getId()))
            .andExpect(jsonPath("$.nodes[0].type").value("PRODUCT"))
            .andExpect(jsonPath("$.nodes[0].parentKey").doesNotExist())
            .andExpect(jsonPath("$.nodes[0].archived").value(true))
            .andExpect(jsonPath("$.nodes[0].lastActivity").doesNotExist())
            .andExpect(jsonPath("$.nodes[1].lastActivity.byLogin").value(OWNER_LOGIN))
            .andExpect(jsonPath("$.nodes[1].links[0].name").value("Home"))
            .andExpect(jsonPath("$.nodes[4].key").value(oppKey))
            .andExpect(jsonPath("$.nodes[4].id").value(f.oppTop.getId()))
            .andExpect(jsonPath("$.nodes[4].parentKey").value("outcome-" + f.outcome1.getId()))
            .andExpect(jsonPath("$.nodes[4].title").value("opp-top"))
            .andExpect(jsonPath("$.nodes[4].notes").value("notes on opp"))
            .andExpect(jsonPath("$.nodes[4].status").value("VALIDATED"))
            .andExpect(jsonPath("$.nodes[4].priority").value(70))
            .andExpect(jsonPath("$.nodes[4].valueRating").value(4))
            .andExpect(jsonPath("$.nodes[4].sortOrder").value(1))
            .andExpect(jsonPath("$.nodes[4].commentCount").value(2))
            .andExpect(jsonPath("$.nodes[4].createdDate").exists())
            .andExpect(jsonPath("$.nodes[4].links[*].name", contains("Board", "Spec")))
            .andExpect(jsonPath("$.nodes[4].links[0].url").value("https://example.com/board"))
            .andExpect(jsonPath("$.nodes[4].links[0].id").value(f.linkFirst.getId()))
            .andExpect(jsonPath("$.nodes[4].questions[*].text", contains("First?", "Second?")))
            .andExpect(jsonPath("$.nodes[4].questions[0].id").value(f.q1.getId()))
            .andExpect(jsonPath("$.nodes[4].questions[0].done").value(false))
            .andExpect(jsonPath("$.nodes[4].questions[1].done").value(true))
            .andExpect(jsonPath("$.nodes[7].type").value("ASSUMPTION"))
            .andExpect(jsonPath("$.nodes[7].title").value("people want it"))
            .andExpect(jsonPath("$.nodes[7].status").value("TESTING"))
            .andExpect(jsonPath("$.nodes[7].confidence").value(70))
            .andExpect(jsonPath("$.nodes[7].ownerLogin").value(EDITOR_LOGIN));
    }

    // ---------------------------------------------------------------
    // Ordering, parent chain, per-node data
    // ---------------------------------------------------------------

    @Test
    void nodesArePreOrderedWithGroupedSiblingsAndCorrectParentKeys() {
        Fixture f = seedFixture();
        authenticate(OWNER_LOGIN);
        TeamTreeDTO tree = teamTreeService.getTreeForTeam(teamA.getId());

        assertThat(tree.getNodes())
            .extracting(TreeNodeDTO::getKey)
            .containsExactly(
                "product-" + f.productA2.getId(),
                "product-" + f.productA1.getId(),
                "outcome-" + f.outcome1.getId(),
                "opportunity-" + f.oppTop2.getId(),
                "opportunity-" + f.oppTop.getId(),
                "opportunity-" + f.childOpp.getId(),
                "solution-" + f.sol.getId(),
                "assumption-" + f.asm.getId(),
                "evidence-" + f.evAsm.getId(),
                "evidence-" + f.evOpp.getId()
            );

        Map<String, TreeNodeDTO> byKey = tree.getNodes().stream().collect(Collectors.toMap(TreeNodeDTO::getKey, Function.identity()));
        assertThat(byKey.get("outcome-" + f.outcome1.getId()).getParentKey()).isEqualTo("product-" + f.productA1.getId());
        assertThat(byKey.get("opportunity-" + f.oppTop.getId()).getParentKey()).isEqualTo("outcome-" + f.outcome1.getId());
        assertThat(byKey.get("opportunity-" + f.childOpp.getId()).getParentKey()).isEqualTo("opportunity-" + f.oppTop.getId());
        assertThat(byKey.get("solution-" + f.sol.getId()).getParentKey()).isEqualTo("opportunity-" + f.oppTop.getId());
        assertThat(byKey.get("assumption-" + f.asm.getId()).getParentKey()).isEqualTo("solution-" + f.sol.getId());
        assertThat(byKey.get("evidence-" + f.evAsm.getId()).getParentKey()).isEqualTo("assumption-" + f.asm.getId());
        assertThat(byKey.get("evidence-" + f.evOpp.getId()).getParentKey()).isEqualTo("opportunity-" + f.oppTop.getId());

        // Every parentKey refers to a node that appears earlier in the list.
        List<String> keys = tree.getNodes().stream().map(TreeNodeDTO::getKey).toList();
        for (int i = 0; i < keys.size(); i++) {
            String parentKey = tree.getNodes().get(i).getParentKey();
            if (parentKey != null) {
                assertThat(keys.subList(0, i)).as("parent of %s precedes it", keys.get(i)).contains(parentKey);
            }
        }

        // Team B is invisible.
        assertThat(keys).doesNotContain("product-" + f.productB.getId());
    }

    @Test
    void commentCountsLinksQuestionsAndTypeSpecificFields() {
        Fixture f = seedFixture();
        authenticate(OWNER_LOGIN);
        TeamTreeDTO tree = teamTreeService.getTreeForTeam(teamA.getId());
        Map<String, TreeNodeDTO> byKey = tree.getNodes().stream().collect(Collectors.toMap(TreeNodeDTO::getKey, Function.identity()));

        TreeNodeDTO opp = byKey.get("opportunity-" + f.oppTop.getId());
        assertThat(opp.getCommentCount()).isEqualTo(2);
        assertThat(opp.getLinks())
            .extracting(l -> l.name())
            .containsExactly("Board", "Spec");
        assertThat(opp.getQuestions())
            .extracting(q -> q.text())
            .containsExactly("First?", "Second?");
        assertThat(opp.getConfidence()).isNull();
        assertThat(opp.getArchived()).isNull();

        assertThat(byKey.get("assumption-" + f.asm.getId()).getCommentCount()).isEqualTo(1);
        assertThat(byKey.get("outcome-" + f.outcome1.getId()).getCommentCount()).isEqualTo(1);
        assertThat(byKey.get("solution-" + f.sol.getId()).getCommentCount()).isZero();
        assertThat(byKey.get("solution-" + f.sol.getId()).getStatus()).isEqualTo("CANDIDATE");
        assertThat(byKey.get("product-" + f.productA1.getId()).getCommentCount()).isZero();
        assertThat(byKey.get("product-" + f.productA1.getId()).getTitle()).isEqualTo("A1");
        assertThat(byKey.get("product-" + f.productA1.getId()).getArchived()).isFalse();
        assertThat(byKey.get("product-" + f.productA1.getId()).getLinks())
            .extracting(l -> l.url())
            .containsExactly("https://example.com/");
        assertThat(byKey.get("outcome-" + f.outcome1.getId()).getQuestions()).isEmpty();
        assertThat(byKey.get("evidence-" + f.evOpp.getId()).getTitle()).isEqualTo("interview snippet");
        assertThat(byKey.get("evidence-" + f.evOpp.getId()).getStatus()).isNull();
    }

    @Test
    void productLastActivityIsNewestHistoryInItsBranchAndEvidenceThisMonthCountsOnlyThisMonth() {
        Fixture f = seedFixture();
        authenticate(EDITOR_LOGIN);
        TeamTreeDTO tree = teamTreeService.getTreeForTeam(teamA.getId());
        Map<String, TreeNodeDTO> byKey = tree.getNodes().stream().collect(Collectors.toMap(TreeNodeDTO::getKey, Function.identity()));

        TreeNodeDTO a1 = byKey.get("product-" + f.productA1.getId());
        assertThat(a1.getLastActivity()).isNotNull();
        assertThat(a1.getLastActivity().at()).isCloseTo(f.newestHistoryAt, org.assertj.core.api.Assertions.within(Duration.ofMillis(1)));
        assertThat(a1.getLastActivity().byLogin()).isEqualTo(OWNER_LOGIN);
        assertThat(byKey.get("product-" + f.productA2.getId()).getLastActivity()).isNull();
        assertThat(byKey.get("outcome-" + f.outcome1.getId()).getLastActivity()).isNull();

        // evOpp (now) counts; evAsm (40 days ago) and team B's evidence do not.
        assertThat(tree.getEvidenceThisMonth()).isEqualTo(1);
    }

    @Test
    void emptyTeamReturnsNoNodes() {
        authenticate(OWNER_LOGIN);
        TeamTreeDTO tree = teamTreeService.getTreeForTeam(teamA.getId());
        assertThat(tree.getNodes()).isEmpty();
        assertThat(tree.getEvidenceThisMonth()).isZero();
        assertThat(tree.getMembers()).hasSize(3);
    }

    @Test
    void serviceRejectsNonMemberWithoutTouchingData() {
        seedFixture();
        authenticate(OUTSIDER_LOGIN);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> teamTreeService.getTreeForTeam(teamA.getId())).isInstanceOf(
            TeamAccessDeniedException.class
        );
    }

    // ---------------------------------------------------------------
    // Query budget: bounded, independent of the size of the tree.
    // ---------------------------------------------------------------

    @Test
    void queryCountIsBoundedAndIndependentOfTreeSize() {
        seedFixture();
        long smallTreeQueries = countQueriesForTreeRead();

        // Grow the tree a lot: deep nesting plus wide fan-out of every type.
        Product p = persistProduct(teamA, "big", false, 5);
        Outcome o = persistOutcome(p, "big-outcome", 0);
        Opportunity parent = persistOpportunity(o, null, "L1", 0);
        for (int depth = 0; depth < 5; depth++) {
            parent = persistOpportunity(o, parent, "deep" + depth, 0);
        }
        for (int i = 0; i < 20; i++) {
            Opportunity leaf = persistOpportunity(o, parent, "leaf" + i, i);
            Solution s = persistSolution(leaf, "leaf-sol" + i, 0);
            Assumption a = new Assumption()
                .statement("a" + i)
                .status(AssumptionStatus.UNTESTED)
                .confidence(40)
                .sortOrder(0)
                .createdDate(Instant.now())
                .solution(s)
                .owner(editorUser);
            em.persist(a);
            em.persist(new Evidence().title("e" + i).sortOrder(0).createdDate(Instant.now()).assumption(a));
            em.persist(new NodeLink().name("l" + i).url("https://example.com/" + i).sortOrder(0).createdDate(Instant.now()).solution(s));
            em.persist(new OpenQuestion().questionText("q" + i).done(false).sortOrder(0).createdDate(Instant.now()).opportunity(leaf));
            persistComment(ownerUser).assumption(a);
        }
        em.flush();
        em.clear();

        long bigTreeQueries = countQueriesForTreeRead();
        assertThat(bigTreeQueries).as("query count must not grow with the tree").isEqualTo(smallTreeQueries);
        assertThat(bigTreeQueries)
            .as("tree read uses a small fixed number of queries; observed %d", bigTreeQueries)
            .isLessThanOrEqualTo(16);
    }

    @Test
    void largeTreeLoadsQuickly() {
        // ~500 nodes: 5 products × 5 outcomes × 10 opportunities × 1 solution.
        int total = 0;
        for (int pi = 0; pi < 5; pi++) {
            Product product = persistProduct(teamA, "P" + pi, false, pi);
            total++;
            for (int oi = 0; oi < 5; oi++) {
                Outcome outcome = persistOutcome(product, "O" + pi + "-" + oi, oi);
                total++;
                for (int qi = 0; qi < 10; qi++) {
                    Opportunity opp = persistOpportunity(outcome, null, "Op" + pi + "-" + oi + "-" + qi, qi);
                    persistSolution(opp, "Sol" + pi + "-" + oi + "-" + qi, 1);
                    total += 2;
                }
            }
        }
        em.flush();
        em.clear();
        assertThat(total).isGreaterThanOrEqualTo(500);

        authenticate(OWNER_LOGIN);
        long start = System.nanoTime();
        TeamTreeDTO tree = teamTreeService.getTreeForTeam(teamA.getId());
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

        assertThat(tree.getNodes()).hasSize(total);
        assertThat(elapsedMs).as("500-node tree must load well under 2 s; took %d ms", elapsedMs).isLessThan(2000L);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private long countQueriesForTreeRead() {
        em.clear();
        Statistics stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        boolean wasEnabled = stats.isStatisticsEnabled();
        stats.setStatisticsEnabled(true);
        stats.clear();
        try {
            authenticate(OWNER_LOGIN);
            teamTreeService.getTreeForTeam(teamA.getId());
            return stats.getPrepareStatementCount();
        } finally {
            stats.setStatisticsEnabled(wasEnabled);
        }
    }

    private Team persistTeam(String name) {
        Team t = new Team().name(name).description("d").createdDate(Instant.now());
        em.persist(t);
        em.flush();
        return t;
    }

    private User persistUser(String login, String firstName, String lastName) {
        return userRepository
            .findOneByLogin(login)
            .orElseGet(() -> {
                User u = new User();
                u.setId(UUID.randomUUID().toString());
                u.setLogin(login);
                u.setActivated(true);
                u.setEmail(login + "@example.com");
                u.setFirstName(firstName);
                u.setLastName(lastName);
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

    private Product persistProduct(Team team, String name, boolean archived, int sortOrder) {
        Product p = new Product().name(name).description("d").archived(archived).sortOrder(sortOrder).createdDate(Instant.now()).team(team);
        em.persist(p);
        return p;
    }

    private Outcome persistOutcome(Product product, String title, int sortOrder) {
        Outcome o = new Outcome().title(title).sortOrder(sortOrder).createdDate(Instant.now()).product(product);
        em.persist(o);
        return o;
    }

    private Opportunity persistOpportunity(Outcome outcome, Opportunity parent, String title, int sortOrder) {
        Opportunity op = new Opportunity()
            .title(title)
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

    private Solution persistSolution(Opportunity opportunity, String title, int sortOrder) {
        Solution s = new Solution()
            .title(title)
            .status(SolutionStatus.CANDIDATE)
            .sortOrder(sortOrder)
            .createdDate(Instant.now())
            .opportunity(opportunity);
        em.persist(s);
        return s;
    }

    /** Persists a comment; the caller attaches it to a node through the returned builder before the next flush. */
    private Comment persistComment(User author) {
        Comment c = new Comment().body("hi").createdDate(Instant.now()).author(author);
        em.persist(c);
        return c;
    }

    private void persistHistory(TreeNodeType type, Long nodeId, Instant at, User author) {
        em.persist(
            new NodeHistory()
                .nodeType(type)
                .nodeId(nodeId)
                .eventType(HistoryEventType.STATUS_CHANGED)
                .summary("x")
                .createdDate(at)
                .author(author)
        );
    }

    private static void authenticate(String login) {
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(login, "n/a", List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
