package com.opportunity.tree.web.rest;

import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.NodeHistory;
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
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Persists OST tree fixtures for the tree write / move integration tests. */
final class OstTreeTestData {

    private final EntityManager em;

    OstTreeTestData(EntityManager em) {
        this.em = em;
    }

    Team team(String name) {
        Team t = new Team().name(name).description("d").createdDate(Instant.now());
        em.persist(t);
        return t;
    }

    User user(String login) {
        List<User> existing = em
            .createQuery("select u from User u where u.login = :l", User.class)
            .setParameter("l", login)
            .getResultList();
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
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

    void member(Team t, User u, TeamRole role) {
        TeamMember tm = new TeamMember().role(role).joinedDate(Instant.now());
        tm.setTeam(t);
        tm.setUser(u);
        em.persist(tm);
    }

    Product product(Team team, String name, int sortOrder) {
        Product p = new Product().name(name).description("d").archived(false).sortOrder(sortOrder).createdDate(Instant.now()).team(team);
        em.persist(p);
        return p;
    }

    Outcome outcome(Product product, String title, int sortOrder) {
        Outcome o = new Outcome().title(title).sortOrder(sortOrder).createdDate(Instant.now()).product(product);
        em.persist(o);
        return o;
    }

    Opportunity opportunity(Outcome outcome, Opportunity parent, String title, int sortOrder) {
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

    Solution solution(Opportunity opportunity, String title, int sortOrder) {
        Solution s = new Solution()
            .title(title)
            .status(SolutionStatus.CANDIDATE)
            .sortOrder(sortOrder)
            .createdDate(Instant.now())
            .opportunity(opportunity);
        em.persist(s);
        return s;
    }

    Assumption assumption(Solution solution, String statement, int sortOrder) {
        Assumption a = new Assumption()
            .statement(statement)
            .status(AssumptionStatus.UNTESTED)
            .confidence(40)
            .sortOrder(sortOrder)
            .createdDate(Instant.now())
            .solution(solution);
        em.persist(a);
        return a;
    }

    Evidence evidence(Opportunity opportunity, Assumption assumption, String title, int sortOrder) {
        Evidence e = new Evidence()
            .title(title)
            .sortOrder(sortOrder)
            .createdDate(Instant.now())
            .opportunity(opportunity)
            .assumption(assumption);
        em.persist(e);
        return e;
    }

    List<NodeHistory> history(TreeNodeType type, Long nodeId) {
        return em
            .createQuery("select h from NodeHistory h where h.nodeType = :t and h.nodeId = :id order by h.id", NodeHistory.class)
            .setParameter("t", type)
            .setParameter("id", nodeId)
            .getResultList();
    }

    List<NodeHistory> history(TreeNodeType type, Long nodeId, HistoryEventType event) {
        return history(type, nodeId)
            .stream()
            .filter(h -> h.getEventType() == event)
            .toList();
    }
}
