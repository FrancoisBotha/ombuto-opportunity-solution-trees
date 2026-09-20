package com.opportunity.tree.web.rest;

import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Removes the teams, users and whole OST subtree a non-transactional integration test seeded.
 *
 * <p><strong>Why this exists.</strong> {@code @AutoConfigureMockMvc} integration tests such as
 * {@link TreeChangeBroadcastIT} and {@link TreeCollaborationBroadcastIT} are deliberately
 * <em>not</em> {@code @Transactional} — they have to let the request transaction commit so the
 * {@code AFTER_COMMIT} broadcast fires. That means everything their {@code @BeforeEach} seeds is
 * committed and survives the test class, leaking into every later test in the same Surefire/Failsafe
 * JVM. In particular the leftover {@code team_member} rows made {@code UserResourceIT} fail with
 * {@code DataIntegrityViolationException: fk_team_member__user_id} when it cleaned up users. A
 * matching {@code @AfterEach} that calls this helper is the fix; {@code TreeTopicChannelInterceptorIT}
 * already followed the same pattern.
 *
 * <p>Deletes run as native statements in FK-safe order — join tables and leaf rows first, then the
 * node tree from the leaves up, then products, memberships, teams and finally the users.
 */
public final class OstTreeTestCleanup {

    private static final String PRODUCTS = "select id from product where team_id in (:teamIds)";
    private static final String OUTCOMES = "select id from outcome where product_id in (" + PRODUCTS + ")";
    private static final String OPPORTUNITIES = "select id from opportunity where outcome_id in (" + OUTCOMES + ")";
    private static final String SOLUTIONS = "select id from solution where opportunity_id in (" + OPPORTUNITIES + ")";
    private static final String ASSUMPTIONS = "select id from assumption where solution_id in (" + SOLUTIONS + ")";
    private static final String EVIDENCE =
        "select id from evidence where opportunity_id in (" + OPPORTUNITIES + ") or assumption_id in (" + ASSUMPTIONS + ")";
    private static final String TAGS = "select id from tag where team_id in (:teamIds)";
    private static final String INTERVIEWS = "select id from interview where product_id in (" + PRODUCTS + ")";
    private static final String USERS = "select id from jhi_user where login in (:logins)";

    /** Ordered so that every statement only removes rows nothing else still points at. */
    private static final List<String> STATEMENTS = List.of(
        "delete from rel_opportunity__tag where opportunity_id in (" + OPPORTUNITIES + ") or tag_id in (" + TAGS + ")",
        "delete from rel_solution__tag where solution_id in (" + SOLUTIONS + ") or tag_id in (" + TAGS + ")",
        "delete from rel_opportunity__interview where opportunity_id in (" + OPPORTUNITIES + ") or interview_id in (" + INTERVIEWS + ")",
        "delete from comment where author_id in (" +
            USERS +
            ") or outcome_id in (" +
            OUTCOMES +
            ") or opportunity_id in (" +
            OPPORTUNITIES +
            ") or solution_id in (" +
            SOLUTIONS +
            ") or assumption_id in (" +
            ASSUMPTIONS +
            ") or evidence_id in (" +
            EVIDENCE +
            ")",
        "delete from node_link where product_id in (" +
            PRODUCTS +
            ") or outcome_id in (" +
            OUTCOMES +
            ") or opportunity_id in (" +
            OPPORTUNITIES +
            ") or solution_id in (" +
            SOLUTIONS +
            ") or assumption_id in (" +
            ASSUMPTIONS +
            ") or evidence_id in (" +
            EVIDENCE +
            ")",
        "delete from open_question where opportunity_id in (" + OPPORTUNITIES + ")",
        "delete from node_history where author_id in (" + USERS + ")",
        "delete from evidence where opportunity_id in (" + OPPORTUNITIES + ") or assumption_id in (" + ASSUMPTIONS + ")",
        "delete from assumption where solution_id in (" + SOLUTIONS + ")",
        "delete from solution where opportunity_id in (" + OPPORTUNITIES + ")",
        "delete from opportunity where outcome_id in (" + OUTCOMES + ")",
        "delete from outcome where product_id in (" + PRODUCTS + ")",
        "delete from interview where product_id in (" + PRODUCTS + ")",
        "delete from tag where team_id in (:teamIds)",
        "delete from product where team_id in (:teamIds)",
        "delete from team_member where team_id in (:teamIds) or user_id in (" + USERS + ")",
        "delete from team where id in (:teamIds)",
        "delete from jhi_user_authority where user_id in (" + USERS + ")",
        "delete from jhi_user where id in (" + USERS + ")"
    );

    private OstTreeTestCleanup() {}

    /**
     * Deletes everything reachable from {@code teamIds} plus the users with {@code logins}, in one
     * transaction. Ids/logins that no longer exist are simply no-ops, so it is safe to call after a
     * test that failed half way through its fixture.
     */
    public static void removeTeamsAndUsers(
        PlatformTransactionManager txMgr,
        EntityManager em,
        Collection<Long> teamIds,
        Collection<String> logins
    ) {
        List<Long> teams = teamIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        List<String> users = logins.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (teams.isEmpty() && users.isEmpty()) {
            return;
        }
        // Empty IN-lists are not valid SQL; substitute an id/login that can never match.
        List<Long> teamParam = teams.isEmpty() ? List.of(Long.MIN_VALUE) : teams;
        List<String> loginParam = users.isEmpty() ? List.of("\u0000-no-such-login") : users;

        new TransactionTemplate(txMgr).execute(status -> {
            em.clear();
            for (String sql : STATEMENTS) {
                var query = em.createNativeQuery(sql);
                if (sql.contains(":teamIds")) {
                    query.setParameter("teamIds", teamParam);
                }
                if (sql.contains(":logins")) {
                    query.setParameter("logins", loginParam);
                }
                query.executeUpdate();
            }
            return null;
        });
    }
}
