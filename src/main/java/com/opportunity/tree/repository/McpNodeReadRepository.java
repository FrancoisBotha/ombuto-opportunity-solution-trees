package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Team;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    // Node basics: [id, title, description, status, priority, valueRating,
    // confidence, ownerLogin, createdDate, lastModifiedDate]. Non-applicable values are null.
    @Query(
        "select p.id, p.name, p.description, cast(null as string), cast(null as integer), cast(null as integer), cast(null as integer), cast(null as string), p.createdDate, cast(null as timestamp) from Product p where p.id = :id"
    )
    List<Object[]> findProductBasics(@Param("id") Long id);

    @Query(
        "select o.id, o.title, o.description, cast(null as string), cast(null as integer), cast(null as integer), cast(null as integer), ow.login, o.createdDate, o.lastModifiedDate from Outcome o left join o.owner ow where o.id = :id"
    )
    List<Object[]> findOutcomeBasics(@Param("id") Long id);

    @Query(
        "select op.id, op.title, op.description, cast(op.status as string), op.priority, op.valuerating, cast(null as integer), ow.login, op.createdDate, op.lastModifiedDate from Opportunity op left join op.owner ow where op.id = :id"
    )
    List<Object[]> findOpportunityBasics(@Param("id") Long id);

    @Query(
        "select s.id, s.title, s.description, cast(s.status as string), cast(null as integer), cast(null as integer), cast(null as integer), ow.login, s.createdDate, s.lastModifiedDate from Solution s left join s.owner ow where s.id = :id"
    )
    List<Object[]> findSolutionBasics(@Param("id") Long id);

    @Query(
        "select a.id, a.statement, a.description, cast(a.status as string), cast(null as integer), cast(null as integer), a.confidence, ow.login, a.createdDate, a.lastModifiedDate from Assumption a left join a.owner ow where a.id = :id"
    )
    List<Object[]> findAssumptionBasics(@Param("id") Long id);

    @Query(
        "select e.id, e.title, e.description, cast(null as string), cast(null as integer), cast(null as integer), cast(null as integer), cast(null as string), e.createdDate, e.lastModifiedDate from Evidence e where e.id = :id"
    )
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
            " where i.product.id = :productId order by i.interviewDate desc, i.id desc",
        countQuery = "select count(i) from Interview i where i.product.id = :productId"
    )
    Page<com.opportunity.tree.domain.Interview> findInterviewsByProductId(@Param("productId") Long productId, Pageable pageable);

    @Query(
        value = "select i from Interview i left join fetch i.product left join fetch i.interviewer" +
            " where i.product.team.id = :teamId order by i.interviewDate desc, i.id desc",
        countQuery = "select count(i) from Interview i where i.product.team.id = :teamId"
    )
    Page<com.opportunity.tree.domain.Interview> findInterviewsByTeamId(@Param("teamId") Long teamId, Pageable pageable);

    @Query(
        value = "select i from Interview i join i.opportunities op left join fetch i.product left join fetch i.interviewer" +
            " where op.id = :opportunityId and i.product.team.id = :teamId order by i.interviewDate desc, i.id desc",
        countQuery = "select count(i) from Interview i join i.opportunities op where op.id = :opportunityId and i.product.team.id = :teamId"
    )
    Page<com.opportunity.tree.domain.Interview> findInterviewsByOpportunityId(
        @Param("opportunityId") Long opportunityId,
        @Param("teamId") Long teamId,
        Pageable pageable
    );

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

    // ---------------------------------------------------------------------
    // Search projections for search_nodes. Each row: [type, id, title, description,
    // status, teamId, teamName]. Team scoping happens through the :teamIds clause,
    // which SearchTool always populates from TeamAccessService — a caller can only
    // ever match nodes in a team they are a member of.
    // ---------------------------------------------------------------------

    @Query(
        "select 'PRODUCT', p.id, p.name, p.description, cast(null as string), p.team.id, p.team.name from Product p" +
            " where p.team.id in :teamIds" +
            " and (lower(p.name) like :q or (p.description is not null and lower(cast(p.description as string)) like :q))"
    )
    List<Object[]> searchProducts(@Param("teamIds") Collection<Long> teamIds, @Param("q") String q);

    @Query(
        "select 'OUTCOME', o.id, o.title, o.description, cast(null as string), o.product.team.id, o.product.team.name from Outcome o" +
            " where o.product.team.id in :teamIds" +
            " and (lower(o.title) like :q or (o.description is not null and lower(cast(o.description as string)) like :q))"
    )
    List<Object[]> searchOutcomes(@Param("teamIds") Collection<Long> teamIds, @Param("q") String q);

    @Query(
        "select 'OPPORTUNITY', op.id, op.title, op.description, cast(op.status as string)," +
            " op.outcome.product.team.id, op.outcome.product.team.name from Opportunity op" +
            " where op.outcome.product.team.id in :teamIds" +
            " and (lower(op.title) like :q or (op.description is not null and lower(cast(op.description as string)) like :q))"
    )
    List<Object[]> searchOpportunities(@Param("teamIds") Collection<Long> teamIds, @Param("q") String q);

    @Query(
        "select 'SOLUTION', s.id, s.title, s.description, cast(s.status as string)," +
            " s.opportunity.outcome.product.team.id, s.opportunity.outcome.product.team.name from Solution s" +
            " where s.opportunity.outcome.product.team.id in :teamIds" +
            " and (lower(s.title) like :q or (s.description is not null and lower(cast(s.description as string)) like :q))"
    )
    List<Object[]> searchSolutions(@Param("teamIds") Collection<Long> teamIds, @Param("q") String q);

    @Query(
        "select 'ASSUMPTION', a.id, a.statement, a.description, cast(a.status as string)," +
            " a.solution.opportunity.outcome.product.team.id, a.solution.opportunity.outcome.product.team.name" +
            " from Assumption a" +
            " where a.solution.opportunity.outcome.product.team.id in :teamIds" +
            " and (lower(a.statement) like :q or (a.description is not null and lower(cast(a.description as string)) like :q))"
    )
    List<Object[]> searchAssumptions(@Param("teamIds") Collection<Long> teamIds, @Param("q") String q);

    @Query(
        "select 'EVIDENCE', e.id, e.title, e.description, cast(null as string)," +
            " coalesce(op.outcome.product.team.id, a.solution.opportunity.outcome.product.team.id)," +
            " coalesce(op.outcome.product.team.name, a.solution.opportunity.outcome.product.team.name)" +
            " from Evidence e left join e.opportunity op left join e.assumption a" +
            " where (op.outcome.product.team.id in :teamIds" +
            "        or a.solution.opportunity.outcome.product.team.id in :teamIds)" +
            " and (lower(e.title) like :q or (e.description is not null and lower(cast(e.description as string)) like :q))"
    )
    List<Object[]> searchEvidence(@Param("teamIds") Collection<Long> teamIds, @Param("q") String q);

    // Context hits: [type, sourceId, parentTitle, matchedText, unused,
    // teamId, teamName, parentType, parentId]. Every query scopes at the DB layer.
    @Query(
        "select 'INTERVIEW_NOTE', i.id, i.title, cast(i.notes as string), cast(null as string), i.product.team.id, i.product.team.name, 'PRODUCT', i.product.id from Interview i where i.product.team.id in :teamIds and i.notes is not null and lower(cast(i.notes as string)) like :q"
    )
    List<Object[]> searchInterviewNotes(@Param("teamIds") Collection<Long> teamIds, @Param("q") String q);

    @Query(
        "select 'OPEN_QUESTION', q.id, op.title, q.questionText, cast(null as string), op.outcome.product.team.id, op.outcome.product.team.name, 'OPPORTUNITY', op.id from OpenQuestion q join q.opportunity op where op.outcome.product.team.id in :teamIds and lower(q.questionText) like :q"
    )
    List<Object[]> searchOpenQuestions(@Param("teamIds") Collection<Long> teamIds, @Param("q") String q);

    @Query(
        "select 'NODE_COMMENT', c.id, o.title, cast(c.body as string), cast(null as string), o.product.team.id, o.product.team.name, 'OUTCOME', o.id from Comment c join c.outcome o where o.product.team.id in :teamIds and lower(cast(c.body as string)) like :q"
    )
    List<Object[]> searchOutcomeComments(@Param("teamIds") Collection<Long> teamIds, @Param("q") String q);

    @Query(
        "select 'NODE_COMMENT', c.id, op.title, cast(c.body as string), cast(null as string), op.outcome.product.team.id, op.outcome.product.team.name, 'OPPORTUNITY', op.id from Comment c join c.opportunity op where op.outcome.product.team.id in :teamIds and lower(cast(c.body as string)) like :q"
    )
    List<Object[]> searchOpportunityComments(@Param("teamIds") Collection<Long> teamIds, @Param("q") String q);

    @Query(
        "select 'NODE_COMMENT', c.id, s.title, cast(c.body as string), cast(null as string), s.opportunity.outcome.product.team.id, s.opportunity.outcome.product.team.name, 'SOLUTION', s.id from Comment c join c.solution s where s.opportunity.outcome.product.team.id in :teamIds and lower(cast(c.body as string)) like :q"
    )
    List<Object[]> searchSolutionComments(@Param("teamIds") Collection<Long> teamIds, @Param("q") String q);

    @Query(
        "select 'NODE_COMMENT', c.id, a.statement, cast(c.body as string), cast(null as string), a.solution.opportunity.outcome.product.team.id, a.solution.opportunity.outcome.product.team.name, 'ASSUMPTION', a.id from Comment c join c.assumption a where a.solution.opportunity.outcome.product.team.id in :teamIds and lower(cast(c.body as string)) like :q"
    )
    List<Object[]> searchAssumptionComments(@Param("teamIds") Collection<Long> teamIds, @Param("q") String q);

    @Query(
        "select 'NODE_COMMENT', c.id, e.title, cast(c.body as string), cast(null as string), coalesce(op.outcome.product.team.id, a.solution.opportunity.outcome.product.team.id), coalesce(op.outcome.product.team.name, a.solution.opportunity.outcome.product.team.name), 'EVIDENCE', e.id from Comment c join c.evidence e left join e.opportunity op left join e.assumption a where (op.outcome.product.team.id in :teamIds or a.solution.opportunity.outcome.product.team.id in :teamIds) and lower(cast(c.body as string)) like :q"
    )
    List<Object[]> searchEvidenceComments(@Param("teamIds") Collection<Long> teamIds, @Param("q") String q);
}
