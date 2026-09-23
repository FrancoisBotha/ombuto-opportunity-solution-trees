package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.NodeLink;
import com.opportunity.tree.domain.OpenQuestion;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Custom (hand-written, NOT generated) read queries behind the whole-team tree
 * ({@code TeamTreeService}). Every method loads one kind of row for a whole
 * team in a single statement, so assembling a tree costs a fixed number of
 * queries regardless of its size.
 *
 * <p>Methods taking id collections must never be passed an empty collection —
 * the caller substitutes a sentinel. Kept in its own interface so regenerating
 * the entities from {@code ombuto.jdl} cannot overwrite it.
 */
public interface TeamTreeRepository extends org.springframework.data.repository.Repository<Team, Long> {
    @Query("select tm from TeamMember tm join fetch tm.user where tm.team.id = :teamId")
    List<TeamMember> findMembersWithUser(@Param("teamId") Long teamId);

    @Query("select p from Product p where p.team.id = :teamId")
    List<Product> findProducts(@Param("teamId") Long teamId);

    /** Rows: [Outcome, ownerLogin]. */
    @Query("select o, ow.login from Outcome o left join o.owner ow where o.product.team.id = :teamId")
    List<Object[]> findOutcomesWithOwner(@Param("teamId") Long teamId);

    /** Rows: [Opportunity, ownerLogin]. */
    @Query("select op, ow.login from Opportunity op left join op.owner ow where op.outcome.product.team.id = :teamId")
    List<Object[]> findOpportunitiesWithOwner(@Param("teamId") Long teamId);

    /** Rows: [Solution, ownerLogin]. */
    @Query("select s, ow.login from Solution s left join s.owner ow where s.opportunity.outcome.product.team.id = :teamId")
    List<Object[]> findSolutionsWithOwner(@Param("teamId") Long teamId);

    /** Rows: [Assumption, ownerLogin]. */
    @Query("select a, ow.login from Assumption a left join a.owner ow where a.solution.opportunity.outcome.product.team.id = :teamId")
    List<Object[]> findAssumptionsWithOwner(@Param("teamId") Long teamId);

    /** Evidence under an opportunity or an assumption of the team. */
    @Query(
        "select e from Evidence e" +
            " left join e.opportunity op1 left join op1.outcome oc1 left join oc1.product p1" +
            " left join e.assumption a left join a.solution s left join s.opportunity op2" +
            " left join op2.outcome oc2 left join oc2.product p2" +
            " where p1.team.id = :teamId or p2.team.id = :teamId"
    )
    List<Evidence> findEvidence(@Param("teamId") Long teamId);

    /** Rows: [outcomeId, opportunityId, solutionId, assumptionId, evidenceId, count] — one non-null id per row. */
    @Query(
        "select o.id, op.id, s.id, a.id, e.id, count(c) from Comment c" +
            " left join c.outcome o left join c.opportunity op left join c.solution s" +
            " left join c.assumption a left join c.evidence e" +
            " where o.id in :outcomeIds or op.id in :opportunityIds or s.id in :solutionIds" +
            " or a.id in :assumptionIds or e.id in :evidenceIds" +
            " group by o.id, op.id, s.id, a.id, e.id"
    )
    List<Object[]> countComments(
        @Param("outcomeIds") Collection<Long> outcomeIds,
        @Param("opportunityIds") Collection<Long> opportunityIds,
        @Param("solutionIds") Collection<Long> solutionIds,
        @Param("assumptionIds") Collection<Long> assumptionIds,
        @Param("evidenceIds") Collection<Long> evidenceIds
    );

    /** Links of the given nodes; the owning node is read from the (lazy) association ids. */
    @Query(
        "select l from NodeLink l" +
            " left join l.product p left join l.outcome o left join l.opportunity op" +
            " left join l.solution s left join l.assumption a left join l.evidence e" +
            " where p.id in :productIds or o.id in :outcomeIds or op.id in :opportunityIds" +
            " or s.id in :solutionIds or a.id in :assumptionIds or e.id in :evidenceIds" +
            " order by l.sortOrder asc, l.id asc"
    )
    List<NodeLink> findLinks(
        @Param("productIds") Collection<Long> productIds,
        @Param("outcomeIds") Collection<Long> outcomeIds,
        @Param("opportunityIds") Collection<Long> opportunityIds,
        @Param("solutionIds") Collection<Long> solutionIds,
        @Param("assumptionIds") Collection<Long> assumptionIds,
        @Param("evidenceIds") Collection<Long> evidenceIds
    );

    @Query("select q from OpenQuestion q where q.opportunity.id in :opportunityIds order by q.sortOrder asc, q.id asc")
    List<OpenQuestion> findQuestions(@Param("opportunityIds") Collection<Long> opportunityIds);

    /**
     * Rows: [productId, outcomeId, opportunityId, solutionId, assumptionId, evidenceId, count] —
     * exactly one non-null id per row. Bodies are never selected, so this query is safe for the
     * tree read (NFR-024). Never pass an empty collection — the caller substitutes a sentinel.
     */
    @Query(
        "select p.id, o.id, op.id, s.id, a.id, e.id, count(t) from MeetingTranscript t" +
            " left join t.product p left join t.outcome o left join t.opportunity op" +
            " left join t.solution s left join t.assumption a left join t.evidence e" +
            " where p.id in :productIds or o.id in :outcomeIds or op.id in :opportunityIds" +
            " or s.id in :solutionIds or a.id in :assumptionIds or e.id in :evidenceIds" +
            " group by p.id, o.id, op.id, s.id, a.id, e.id"
    )
    List<Object[]> countTranscripts(
        @Param("productIds") Collection<Long> productIds,
        @Param("outcomeIds") Collection<Long> outcomeIds,
        @Param("opportunityIds") Collection<Long> opportunityIds,
        @Param("solutionIds") Collection<Long> solutionIds,
        @Param("assumptionIds") Collection<Long> assumptionIds,
        @Param("evidenceIds") Collection<Long> evidenceIds
    );

    /**
     * The newest history row of every given node. Rows: [nodeType, nodeId,
     * createdDate, authorLogin]. Ties on createdDate may yield more than one row
     * per node.
     */
    @Query(
        "select h.nodeType, h.nodeId, h.createdDate, au.login from NodeHistory h left join h.author au" +
            " where ((h.nodeType = com.opportunity.tree.domain.enumeration.TreeNodeType.PRODUCT and h.nodeId in :productIds)" +
            " or (h.nodeType = com.opportunity.tree.domain.enumeration.TreeNodeType.OUTCOME and h.nodeId in :outcomeIds)" +
            " or (h.nodeType = com.opportunity.tree.domain.enumeration.TreeNodeType.OPPORTUNITY and h.nodeId in :opportunityIds)" +
            " or (h.nodeType = com.opportunity.tree.domain.enumeration.TreeNodeType.SOLUTION and h.nodeId in :solutionIds)" +
            " or (h.nodeType = com.opportunity.tree.domain.enumeration.TreeNodeType.ASSUMPTION and h.nodeId in :assumptionIds)" +
            " or (h.nodeType = com.opportunity.tree.domain.enumeration.TreeNodeType.EVIDENCE and h.nodeId in :evidenceIds))" +
            " and h.createdDate = (select max(h2.createdDate) from NodeHistory h2" +
            " where h2.nodeType = h.nodeType and h2.nodeId = h.nodeId)"
    )
    List<Object[]> findLatestHistory(
        @Param("productIds") Collection<Long> productIds,
        @Param("outcomeIds") Collection<Long> outcomeIds,
        @Param("opportunityIds") Collection<Long> opportunityIds,
        @Param("solutionIds") Collection<Long> solutionIds,
        @Param("assumptionIds") Collection<Long> assumptionIds,
        @Param("evidenceIds") Collection<Long> evidenceIds
    );
}
