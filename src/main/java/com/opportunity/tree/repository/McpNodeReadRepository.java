package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Team;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Custom (hand-written, NOT generated) read-only lookups for the MCP tools. Kept in its own
 * interface so regenerating the entities from {@code ombuto.jdl} cannot overwrite the queries.
 *
 * <p>Each method is a scalar / projection query used to assemble a compact node description
 * or interview summary without loading the full aggregate.
 */
public interface McpNodeReadRepository extends org.springframework.data.repository.Repository<Team, Long> {
    // Node basics: [id, title, description, status]
    @Query("select p.id, p.name, p.description, cast(null as string) from Product p where p.id = :id")
    List<Object[]> findProductBasics(@Param("id") Long id);

    @Query("select o.id, o.title, o.description, cast(null as string) from Outcome o where o.id = :id")
    List<Object[]> findOutcomeBasics(@Param("id") Long id);

    @Query("select op.id, op.title, op.description, cast(op.status as string) from Opportunity op where op.id = :id")
    List<Object[]> findOpportunityBasics(@Param("id") Long id);

    @Query("select s.id, s.title, s.description, cast(s.status as string) from Solution s where s.id = :id")
    List<Object[]> findSolutionBasics(@Param("id") Long id);

    @Query("select a.id, a.statement, a.description, cast(a.status as string) from Assumption a where a.id = :id")
    List<Object[]> findAssumptionBasics(@Param("id") Long id);

    @Query("select e.id, e.title, e.description, cast(null as string) from Evidence e where e.id = :id")
    List<Object[]> findEvidenceBasics(@Param("id") Long id);

    // Parent projections: [parentType, parentId, parentTitle]
    @Query("select 'PRODUCT', o.product.id, o.product.name from Outcome o where o.id = :id")
    List<Object[]> findOutcomeParent(@Param("id") Long id);

    @Query(
        "select case when parent.id is null then 'OUTCOME' else 'OPPORTUNITY' end," +
            " coalesce(parent.id, outcome.id)," +
            " coalesce(parent.title, outcome.title)" +
            " from Opportunity op left join op.parent parent left join op.outcome outcome where op.id = :id"
    )
    List<Object[]> findOpportunityParent(@Param("id") Long id);

    @Query("select 'OPPORTUNITY', s.opportunity.id, s.opportunity.title from Solution s where s.id = :id")
    List<Object[]> findSolutionParent(@Param("id") Long id);

    @Query("select 'SOLUTION', a.solution.id, a.solution.title from Assumption a where a.id = :id")
    List<Object[]> findAssumptionParent(@Param("id") Long id);

    @Query(
        "select case when opportunity.id is not null then 'OPPORTUNITY' else 'ASSUMPTION' end," +
            " coalesce(opportunity.id, assumption.id)," +
            " coalesce(opportunity.title, assumption.statement)" +
            " from Evidence e left join e.opportunity opportunity left join e.assumption assumption where e.id = :id"
    )
    List<Object[]> findEvidenceParent(@Param("id") Long id);

    // Children projections: [childType, childId, childTitle], ordered by sort_order.
    @Query("select 'OUTCOME', o.id, o.title from Outcome o where o.product.id = :id order by o.sortOrder")
    List<Object[]> findProductChildren(@Param("id") Long id);

    @Query(
        "select 'OPPORTUNITY', op.id, op.title from Opportunity op" +
            " where op.outcome.id = :id and op.parent is null order by op.sortOrder"
    )
    List<Object[]> findOutcomeChildren(@Param("id") Long id);

    @Query("select 'OPPORTUNITY', op.id, op.title from Opportunity op where op.parent.id = :id order by op.sortOrder")
    List<Object[]> findOpportunityChildOpportunities(@Param("id") Long id);

    @Query("select 'SOLUTION', s.id, s.title from Solution s where s.opportunity.id = :id order by s.sortOrder")
    List<Object[]> findOpportunityChildSolutions(@Param("id") Long id);

    @Query("select 'EVIDENCE', e.id, e.title from Evidence e where e.opportunity.id = :id order by e.sortOrder")
    List<Object[]> findOpportunityChildEvidence(@Param("id") Long id);

    @Query("select 'ASSUMPTION', a.id, a.statement from Assumption a where a.solution.id = :id order by a.sortOrder")
    List<Object[]> findSolutionChildren(@Param("id") Long id);

    @Query("select 'EVIDENCE', e.id, e.title from Evidence e where e.assumption.id = :id order by e.sortOrder")
    List<Object[]> findAssumptionChildren(@Param("id") Long id);

    // Counts.
    @Query("select count(c) from Comment c where c.outcome.id = :id")
    long countCommentsForOutcome(@Param("id") Long id);

    @Query("select count(c) from Comment c where c.opportunity.id = :id")
    long countCommentsForOpportunity(@Param("id") Long id);

    @Query("select count(c) from Comment c where c.solution.id = :id")
    long countCommentsForSolution(@Param("id") Long id);

    @Query("select count(c) from Comment c where c.assumption.id = :id")
    long countCommentsForAssumption(@Param("id") Long id);

    @Query("select count(c) from Comment c where c.evidence.id = :id")
    long countCommentsForEvidence(@Param("id") Long id);

    @Query("select count(e) from Evidence e where e.opportunity.id = :id")
    long countEvidenceForOpportunity(@Param("id") Long id);

    @Query("select count(e) from Evidence e where e.assumption.id = :id")
    long countEvidenceForAssumption(@Param("id") Long id);

    // Links: [name, url], ordered by sort_order.
    @Query("select l.name, l.url from NodeLink l where l.product.id = :id order by l.sortOrder asc, l.id asc")
    List<Object[]> findLinksForProduct(@Param("id") Long id);

    @Query("select l.name, l.url from NodeLink l where l.outcome.id = :id order by l.sortOrder asc, l.id asc")
    List<Object[]> findLinksForOutcome(@Param("id") Long id);

    @Query("select l.name, l.url from NodeLink l where l.opportunity.id = :id order by l.sortOrder asc, l.id asc")
    List<Object[]> findLinksForOpportunity(@Param("id") Long id);

    @Query("select l.name, l.url from NodeLink l where l.solution.id = :id order by l.sortOrder asc, l.id asc")
    List<Object[]> findLinksForSolution(@Param("id") Long id);

    @Query("select l.name, l.url from NodeLink l where l.assumption.id = :id order by l.sortOrder asc, l.id asc")
    List<Object[]> findLinksForAssumption(@Param("id") Long id);

    @Query("select l.name, l.url from NodeLink l where l.evidence.id = :id order by l.sortOrder asc, l.id asc")
    List<Object[]> findLinksForEvidence(@Param("id") Long id);

    // Open questions on an opportunity: [id, questionText, done], ordered by sort_order.
    @Query("select q.id, q.questionText, q.done from OpenQuestion q where q.opportunity.id = :id order by q.sortOrder asc, q.id asc")
    List<Object[]> findOpenQuestionsForOpportunity(@Param("id") Long id);

    // Interviews: page by product or by team, ordered by date desc then id desc for stability.
    @Query(
        value = "select i from Interview i left join fetch i.product left join fetch i.interviewer" +
            " where i.product.id = :productId order by i.interviewDate desc, i.id desc"
    )
    List<com.opportunity.tree.domain.Interview> findInterviewsByProductId(@Param("productId") Long productId);

    @Query(
        value = "select i from Interview i left join fetch i.product left join fetch i.interviewer" +
            " where i.product.team.id = :teamId order by i.interviewDate desc, i.id desc"
    )
    List<com.opportunity.tree.domain.Interview> findInterviewsByTeamId(@Param("teamId") Long teamId);

    @Query("select i.product.team.id from Interview i where i.id = :id")
    Optional<Long> findTeamIdOfInterview(@Param("id") Long id);

    // Comments on a node: [id, body, authorLogin, createdDate, editedDate], oldest first.
    @Query(
        "select c.id, c.body, c.author.login, c.createdDate, c.editedDate from Comment c" +
            " where c.outcome.id = :id order by c.createdDate asc, c.id asc"
    )
    List<Object[]> findCommentsForOutcome(@Param("id") Long id);

    @Query(
        "select c.id, c.body, c.author.login, c.createdDate, c.editedDate from Comment c" +
            " where c.opportunity.id = :id order by c.createdDate asc, c.id asc"
    )
    List<Object[]> findCommentsForOpportunity(@Param("id") Long id);

    @Query(
        "select c.id, c.body, c.author.login, c.createdDate, c.editedDate from Comment c" +
            " where c.solution.id = :id order by c.createdDate asc, c.id asc"
    )
    List<Object[]> findCommentsForSolution(@Param("id") Long id);

    @Query(
        "select c.id, c.body, c.author.login, c.createdDate, c.editedDate from Comment c" +
            " where c.assumption.id = :id order by c.createdDate asc, c.id asc"
    )
    List<Object[]> findCommentsForAssumption(@Param("id") Long id);

    @Query(
        "select c.id, c.body, c.author.login, c.createdDate, c.editedDate from Comment c" +
            " where c.evidence.id = :id order by c.createdDate asc, c.id asc"
    )
    List<Object[]> findCommentsForEvidence(@Param("id") Long id);
}
