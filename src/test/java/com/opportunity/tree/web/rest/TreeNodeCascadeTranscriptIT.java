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
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.service.TreeNodeCascadeService;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.transaction.annotation.Transactional;

/**
 * MTRANS-004 — deleting a tree node cascades the deletion to its meeting transcripts, both when the
 * node is deleted directly and when it is removed as a descendant of a higher-up delete.
 */
@IntegrationTest
@Transactional
class TreeNodeCascadeTranscriptIT {

    private static final String OWNER_LOGIN = "trx-cascade-owner";

    @Autowired
    private EntityManager em;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TreeNodeCascadeService cascadeService;

    private Team teamA;
    private User ownerUser;

    @BeforeEach
    void setUp() {
        teamA = persistTeam("Cascade Transcript Team");
        ownerUser = persistUser(OWNER_LOGIN);
        persistMembership(teamA, ownerUser, TeamRole.OWNER);
        authenticate(OWNER_LOGIN);
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
    void deletingProductRemovesTranscriptsFromEveryDescendantNode() {
        Product product = persistProduct(teamA, "prod");
        Product sibling = persistProduct(teamA, "sibling");
        Outcome outcome = persistOutcome(product, "outcome");
        Opportunity opp = persistOpportunity(outcome, null, "opp");
        Solution sol = persistSolution(opp, "sol");
        Assumption asm = persistAssumption(sol);
        Evidence ev = new Evidence().title("ev").sortOrder(0).createdDate(Instant.now()).opportunity(opp);
        em.persist(ev);
        em.flush();

        Long prodT = persistTranscript(t -> t.product(product)).getId();
        Long outT = persistTranscript(t -> t.outcome(outcome)).getId();
        Long oppT = persistTranscript(t -> t.opportunity(opp)).getId();
        Long solT = persistTranscript(t -> t.solution(sol)).getId();
        Long asmT = persistTranscript(t -> t.assumption(asm)).getId();
        Long evT = persistTranscript(t -> t.evidence(ev)).getId();
        Long siblingT = persistTranscript(t -> t.product(sibling)).getId();
        em.flush();

        cascadeService.deleteProduct(product.getId());
        em.flush();
        em.clear();

        assertThat(transcriptExists(prodT)).isFalse();
        assertThat(transcriptExists(outT)).isFalse();
        assertThat(transcriptExists(oppT)).isFalse();
        assertThat(transcriptExists(solT)).isFalse();
        assertThat(transcriptExists(asmT)).isFalse();
        assertThat(transcriptExists(evT)).isFalse();
        // Sibling product's transcript is untouched.
        assertThat(transcriptExists(siblingT)).isTrue();
    }

    @Test
    void deletingOpportunityRemovesTranscriptsOnItAndItsDescendants() {
        Product product = persistProduct(teamA, "prod");
        Outcome outcome = persistOutcome(product, "outcome");
        Opportunity opp = persistOpportunity(outcome, null, "opp");
        Opportunity siblingOpp = persistOpportunity(outcome, null, "sibling-opp");
        Solution sol = persistSolution(opp, "sol");
        Assumption asm = persistAssumption(sol);
        Evidence ev = new Evidence().title("ev").sortOrder(0).createdDate(Instant.now()).assumption(asm);
        em.persist(ev);
        em.flush();

        Long oppT = persistTranscript(t -> t.opportunity(opp)).getId();
        Long solT = persistTranscript(t -> t.solution(sol)).getId();
        Long asmT = persistTranscript(t -> t.assumption(asm)).getId();
        Long evT = persistTranscript(t -> t.evidence(ev)).getId();
        Long siblingT = persistTranscript(t -> t.opportunity(siblingOpp)).getId();
        Long outT = persistTranscript(t -> t.outcome(outcome)).getId();
        em.flush();

        cascadeService.deleteOpportunity(opp.getId());
        em.flush();
        em.clear();

        assertThat(transcriptExists(oppT)).isFalse();
        assertThat(transcriptExists(solT)).isFalse();
        assertThat(transcriptExists(asmT)).isFalse();
        assertThat(transcriptExists(evT)).isFalse();
        // Sibling opportunity and parent outcome are untouched.
        assertThat(transcriptExists(siblingT)).isTrue();
        assertThat(transcriptExists(outT)).isTrue();
    }

    @Test
    void deletingLeafNodeRemovesOnlyItsOwnTranscripts() {
        Product product = persistProduct(teamA, "prod");
        Outcome outcome = persistOutcome(product, "outcome");
        Opportunity opp = persistOpportunity(outcome, null, "opp");
        Evidence ev = new Evidence().title("ev").sortOrder(0).createdDate(Instant.now()).opportunity(opp);
        em.persist(ev);
        em.flush();

        Long evT1 = persistTranscript(t -> t.evidence(ev)).getId();
        Long evT2 = persistTranscript(t -> t.evidence(ev)).getId();
        Long oppT = persistTranscript(t -> t.opportunity(opp)).getId();
        em.flush();

        cascadeService.deleteEvidence(ev.getId());
        em.flush();
        em.clear();

        assertThat(transcriptExists(evT1)).isFalse();
        assertThat(transcriptExists(evT2)).isFalse();
        // Parent opportunity's transcript is untouched.
        assertThat(transcriptExists(oppT)).isTrue();
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private boolean transcriptExists(Long id) {
        Long count = em
            .createQuery("select count(t) from MeetingTranscript t where t.id = :id", Long.class)
            .setParameter("id", id)
            .getSingleResult();
        return count != null && count > 0;
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

    private Assumption persistAssumption(Solution sol) {
        Assumption a = new Assumption()
            .statement("we assume things")
            .status(AssumptionStatus.UNTESTED)
            .confidence(50)
            .sortOrder(0)
            .createdDate(Instant.now())
            .solution(sol);
        em.persist(a);
        return a;
    }

    private MeetingTranscript persistTranscript(java.util.function.Consumer<MeetingTranscript> attach) {
        MeetingTranscript t = new MeetingTranscript()
            .title("kickoff meeting")
            .meetingDate(LocalDate.now())
            .attendees("alice, bob")
            .body("body text")
            .source(MeetingTranscriptSource.PASTED)
            .createdDate(Instant.now())
            .author(ownerUser);
        attach.accept(t);
        em.persist(t);
        return t;
    }

    @SuppressWarnings("unused")
    private static TreeNodeType nothing() {
        return TreeNodeType.PRODUCT;
    }
}
