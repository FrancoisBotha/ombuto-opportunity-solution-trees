package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Team;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Custom (hand-written, NOT generated) lookups that resolve any tree node, node
 * link, open question or comment id up to the id of the team that owns it.
 * Used by {@code TeamAccessService}. Each method is a single projection query
 * and returns empty when the id does not exist.
 *
 * <p>Kept in its own interface so regenerating the entities from
 * {@code ombuto.jdl} cannot overwrite it.
 */
public interface TreeAccessLookupRepository extends org.springframework.data.repository.Repository<Team, Long> {
    /**
     * Every team the user with {@code login} belongs to, with the role: rows of [teamId (Long),
     * role (TeamRole)]. One projection statement that reads the membership's foreign key and never
     * loads a Team entity, so a team deleted concurrently simply drops out instead of failing the
     * request with an ObjectNotFoundException (a lazy/eager team load after the memberships).
     */
    @Query("select m.team.id, m.role from TeamMember m where m.user.login = :login")
    List<Object[]> findTeamRolesOfUser(@Param("login") String login);

    @Query("select p.team.id from Product p where p.id = :id")
    Optional<Long> findTeamIdOfProduct(@Param("id") Long id);

    @Query("select o.product.team.id from Outcome o where o.id = :id")
    Optional<Long> findTeamIdOfOutcome(@Param("id") Long id);

    @Query("select op.outcome.product.team.id from Opportunity op where op.id = :id")
    Optional<Long> findTeamIdOfOpportunity(@Param("id") Long id);

    @Query("select s.opportunity.outcome.product.team.id from Solution s where s.id = :id")
    Optional<Long> findTeamIdOfSolution(@Param("id") Long id);

    @Query("select a.solution.opportunity.outcome.product.team.id from Assumption a where a.id = :id")
    Optional<Long> findTeamIdOfAssumption(@Param("id") Long id);

    /** Evidence hangs off either an opportunity or an assumption. */
    @Query(
        "select coalesce(p1.team.id, p2.team.id) from Evidence e" +
            " left join e.opportunity op1 left join op1.outcome oc1 left join oc1.product p1" +
            " left join e.assumption a left join a.solution s left join s.opportunity op2" +
            " left join op2.outcome oc2 left join oc2.product p2" +
            " where e.id = :id"
    )
    Optional<Long> findTeamIdOfEvidence(@Param("id") Long id);

    /** One row: [productId, outcomeId, opportunityId, solutionId, assumptionId, evidenceId] — exactly one non-null. */
    @Query(
        "select p.id, o.id, op.id, s.id, a.id, e.id from NodeLink l" +
            " left join l.product p left join l.outcome o left join l.opportunity op" +
            " left join l.solution s left join l.assumption a left join l.evidence e" +
            " where l.id = :id"
    )
    List<Object[]> findNodeIdsOfLink(@Param("id") Long id);

    @Query("select q.opportunity.id from OpenQuestion q where q.id = :id")
    Optional<Long> findOpportunityIdOfQuestion(@Param("id") Long id);

    /** One row: [outcomeId, opportunityId, solutionId, assumptionId, evidenceId] — exactly one non-null. */
    @Query(
        "select o.id, op.id, s.id, a.id, e.id from Comment c" +
            " left join c.outcome o left join c.opportunity op left join c.solution s" +
            " left join c.assumption a left join c.evidence e" +
            " where c.id = :id"
    )
    List<Object[]> findNodeIdsOfComment(@Param("id") Long id);
}
