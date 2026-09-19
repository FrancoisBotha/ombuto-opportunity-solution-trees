package com.opportunity.tree.web.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Evidence;
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
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Test data shared by the tree collaboration ITs (links, open questions, chat,
 * history): one team with an owner, an editor and a viewer, an outsider who is
 * a member of another team only, and one node of every type in a single branch
 * (product → outcome → opportunity → solution → assumption → evidence).
 * Everything is persisted in the caller's (rolled-back) test transaction.
 */
final class TreeCollaborationFixture {

    static final String OWNER = "collab-owner";
    static final String EDITOR = "collab-editor";
    static final String VIEWER = "collab-viewer";
    static final String OUTSIDER = "collab-outsider";

    final User owner;
    final User editor;
    final User viewer;
    final User outsider;
    final Team team;
    final Team otherTeam;
    final Product product;
    final Outcome outcome;
    final Opportunity opportunity;
    final Solution solution;
    final Assumption assumption;
    final Evidence evidence;

    private final EntityManager em;

    TreeCollaborationFixture(EntityManager em) {
        this.em = em;
        owner = persistUser(OWNER, "Kara", "Patel");
        editor = persistUser(EDITOR, "Ari", "Reyes");
        viewer = persistUser(VIEWER, "Jo", "Smith");
        outsider = persistUser(OUTSIDER, "Out", "Sider");

        team = new Team().name("Collab Team").description("d").createdDate(Instant.now());
        em.persist(team);
        otherTeam = new Team().name("Collab Other Team").description("d").createdDate(Instant.now());
        em.persist(otherTeam);
        member(team, owner, TeamRole.OWNER);
        member(team, editor, TeamRole.EDITOR);
        member(team, viewer, TeamRole.VIEWER);
        member(otherTeam, outsider, TeamRole.OWNER);

        product = new Product().name("Collab product").archived(false).sortOrder(0).createdDate(Instant.now()).team(team);
        em.persist(product);
        outcome = new Outcome().title("Collab outcome").sortOrder(0).createdDate(Instant.now()).product(product);
        em.persist(outcome);
        opportunity = new Opportunity()
            .title("Collab opportunity")
            .status(OpportunityStatus.UNEXPLORED)
            .valuerating(3)
            .priority(50)
            .sortOrder(0)
            .createdDate(Instant.now())
            .outcome(outcome);
        em.persist(opportunity);
        solution = new Solution()
            .title("Collab solution")
            .status(SolutionStatus.CANDIDATE)
            .sortOrder(0)
            .createdDate(Instant.now())
            .opportunity(opportunity);
        em.persist(solution);
        assumption = new Assumption()
            .statement("Collab assumption")
            .status(AssumptionStatus.UNTESTED)
            .confidence(40)
            .sortOrder(0)
            .createdDate(Instant.now())
            .solution(solution);
        em.persist(assumption);
        evidence = new Evidence().title("Collab evidence").sortOrder(0).createdDate(Instant.now()).assumption(assumption);
        em.persist(evidence);
        em.flush();
    }

    /** Id of this fixture's node of the given type. */
    Long idOf(TreeNodeType type) {
        return switch (type) {
            case PRODUCT -> product.getId();
            case OUTCOME -> outcome.getId();
            case OPPORTUNITY -> opportunity.getId();
            case SOLUTION -> solution.getId();
            case ASSUMPTION -> assumption.getId();
            case EVIDENCE -> evidence.getId();
        };
    }

    /** Path segment for the type, e.g. {@code opportunity}. */
    static String path(TreeNodeType type) {
        return type.name().toLowerCase();
    }

    /** "admin" carries ROLE_ADMIN but is not a team member: admins get no implicit access. */
    static RequestPostProcessor who(String login) {
        return "admin".equals(login) ? user(login).roles("USER", "ADMIN") : user(login);
    }

    static List<TreeNodeType> chatTypes() {
        return List.of(
            TreeNodeType.OUTCOME,
            TreeNodeType.OPPORTUNITY,
            TreeNodeType.SOLUTION,
            TreeNodeType.ASSUMPTION,
            TreeNodeType.EVIDENCE
        );
    }

    long count(String jpql, Object... params) {
        var q = em.createQuery(jpql, Long.class);
        for (int i = 0; i < params.length; i++) {
            q.setParameter(i + 1, params[i]);
        }
        return q.getSingleResult();
    }

    /** Links on any of this fixture's nodes. */
    long linkCount() {
        return count(
            "select count(l) from NodeLink l left join l.product p left join l.outcome o left join l.opportunity op" +
                " left join l.solution s left join l.assumption a left join l.evidence e" +
                " where p.id = ?1 or o.id = ?2 or op.id = ?3 or s.id = ?4 or a.id = ?5 or e.id = ?6",
            product.getId(),
            outcome.getId(),
            opportunity.getId(),
            solution.getId(),
            assumption.getId(),
            evidence.getId()
        );
    }

    /** History rows of any of this fixture's nodes. */
    long historyCount() {
        long total = 0;
        for (TreeNodeType type : TreeNodeType.values()) {
            total += count("select count(h) from NodeHistory h where h.nodeType = ?1 and h.nodeId = ?2", type, idOf(type));
        }
        return total;
    }

    private User persistUser(String login, String first, String last) {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setLogin(login);
        u.setActivated(true);
        u.setEmail(login + "@example.com");
        u.setFirstName(first);
        u.setLastName(last);
        u.setLangKey("en");
        em.persist(u);
        return u;
    }

    private void member(Team t, User u, TeamRole role) {
        TeamMember tm = new TeamMember().role(role).joinedDate(Instant.now());
        tm.setTeam(t);
        tm.setUser(u);
        em.persist(tm);
    }
}
