package com.opportunity.tree.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.MeetingTranscript;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.AssumptionStatus;
import com.opportunity.tree.domain.enumeration.MeetingTranscriptSource;
import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import com.opportunity.tree.domain.enumeration.SolutionStatus;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.service.TeamTreeService;
import com.opportunity.tree.service.dto.tree.TeamTreeDTO;
import com.opportunity.tree.service.dto.tree.TreeNodeDTO;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.transaction.annotation.Transactional;

/**
 * MTRANS-004 — the tree payload carries an accurate {@code transcriptCount} for every node type,
 * with zero when nothing is attached, computed with a bounded number of queries.
 */
@IntegrationTest
@Transactional
class TreeTranscriptCountIT {

    private static final String OWNER_LOGIN = "trx-count-owner";

    @Autowired
    private EntityManager em;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamTreeService teamTreeService;

    private Team teamA;
    private User ownerUser;

    @BeforeEach
    void setUp() {
        teamA = persistTeam("Transcript Count Team");
        ownerUser = persistUser(OWNER_LOGIN);
        persistMembership(teamA, ownerUser, TeamRole.OWNER);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String login) {
        SecurityContextImpl ctx = new SecurityContextImpl();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(login, "n/a", java.util.List.of()));
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    void transcriptCountsCoverEveryNodeTypeAndAreZeroWhenAbsent() {
        Instant now = Instant.now();
        Product product = persistProduct(teamA, "prod");
        Product emptyProduct = persistProduct(teamA, "empty");
        Outcome outcome = persistOutcome(product, "outcome");
        Opportunity opp = persistOpportunity(outcome, null, "opp");
        Solution sol = persistSolution(opp, "sol");
        Assumption asm = new Assumption()
            .statement("we assume things")
            .status(AssumptionStatus.UNTESTED)
            .confidence(50)
            .sortOrder(0)
            .createdDate(now)
            .solution(sol);
        em.persist(asm);
        Evidence ev = new Evidence().title("ev").sortOrder(0).createdDate(now).opportunity(opp);
        em.persist(ev);
        em.flush();

        // Two on product, one on each of outcome/opportunity/solution/assumption/evidence.
        persistTranscript(t -> t.product(product));
        persistTranscript(t -> t.product(product));
        persistTranscript(t -> t.outcome(outcome));
        persistTranscript(t -> t.opportunity(opp));
        persistTranscript(t -> t.solution(sol));
        persistTranscript(t -> t.assumption(asm));
        persistTranscript(t -> t.evidence(ev));
        em.flush();
        em.clear();

        authenticate(OWNER_LOGIN);
        TeamTreeDTO tree = teamTreeService.getTreeForTeam(teamA.getId());
        Map<String, TreeNodeDTO> byKey = tree.getNodes().stream().collect(Collectors.toMap(TreeNodeDTO::getKey, Function.identity()));

        assertThat(byKey.get("product-" + product.getId()).getTranscriptCount()).isEqualTo(2);
        assertThat(byKey.get("product-" + emptyProduct.getId()).getTranscriptCount()).isZero();
        assertThat(byKey.get("outcome-" + outcome.getId()).getTranscriptCount()).isEqualTo(1);
        assertThat(byKey.get("opportunity-" + opp.getId()).getTranscriptCount()).isEqualTo(1);
        assertThat(byKey.get("solution-" + sol.getId()).getTranscriptCount()).isEqualTo(1);
        assertThat(byKey.get("assumption-" + asm.getId()).getTranscriptCount()).isEqualTo(1);
        assertThat(byKey.get("evidence-" + ev.getId()).getTranscriptCount()).isEqualTo(1);

        // Tree DTO has no field for a transcript body, so no body can be carried in the payload.
        // Guard against a regression that adds one: the DTO's reflected fields must never mention "body".
        for (java.lang.reflect.Field field : TreeNodeDTO.class.getDeclaredFields()) {
            assertThat(field.getName().toLowerCase()).doesNotContain("body");
        }
    }

    @Test
    void treeReadUsesBoundedQueryCountWithTranscriptCounts() {
        Product product = persistProduct(teamA, "prod");
        Outcome outcome = persistOutcome(product, "outcome");
        Opportunity opp = persistOpportunity(outcome, null, "opp");
        em.flush();
        persistTranscript(t -> t.outcome(outcome));
        persistTranscript(t -> t.opportunity(opp));
        em.flush();
        em.clear();

        em.clear();
        Statistics stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        boolean wasEnabled = stats.isStatisticsEnabled();
        stats.setStatisticsEnabled(true);
        stats.clear();
        try {
            authenticate(OWNER_LOGIN);
            teamTreeService.getTreeForTeam(teamA.getId());
            // Under 16 queries — the same bound the pre-existing tree-read test asserts on.
            assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(16);
        } finally {
            stats.setStatisticsEnabled(wasEnabled);
        }
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

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
                u.setFirstName("f");
                u.setLastName("l");
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

    private Product persistProduct(Team team, String name) {
        Product p = new Product().name(name).description("d").archived(false).sortOrder(0).createdDate(Instant.now()).team(team);
        em.persist(p);
        return p;
    }

    private Outcome persistOutcome(Product product, String title) {
        Outcome o = new Outcome().title(title).sortOrder(0).createdDate(Instant.now()).product(product);
        em.persist(o);
        return o;
    }

    private Opportunity persistOpportunity(Outcome outcome, Opportunity parent, String title) {
        Opportunity op = new Opportunity()
            .title(title)
            .status(OpportunityStatus.UNEXPLORED)
            .valuerating(3)
            .priority(50)
            .sortOrder(0)
            .createdDate(Instant.now())
            .outcome(outcome)
            .parent(parent);
        em.persist(op);
        return op;
    }

    private Solution persistSolution(Opportunity opp, String title) {
        Solution s = new Solution().title(title).status(SolutionStatus.CANDIDATE).sortOrder(0).createdDate(Instant.now()).opportunity(opp);
        em.persist(s);
        return s;
    }

    private MeetingTranscript persistTranscript(java.util.function.Consumer<MeetingTranscript> attach) {
        MeetingTranscript t = new MeetingTranscript()
            .title("kickoff meeting")
            .meetingDate(LocalDate.now())
            .attendees("alice, bob")
            .body("SECRET-BODY-CONTENT — never leaks to the tree payload")
            .source(MeetingTranscriptSource.PASTED)
            .createdDate(Instant.now())
            .author(ownerUser);
        attach.accept(t);
        em.persist(t);
        return t;
    }
}
