package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Comment;
import com.opportunity.tree.domain.NodeHistory;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Custom (hand-written, NOT generated) queries behind the tree collaboration
 * APIs: links, open questions, comments/chat and the history read. Kept in its
 * own interface so regenerating the entities from {@code ombuto.jdl} cannot
 * overwrite it.
 */
public interface TreeCollaborationRepository extends org.springframework.data.repository.Repository<Comment, Long> {
    // --- next sort order ----------------------------------------------------

    @Query("select max(l.sortOrder) from NodeLink l where l.product.id = :id")
    Integer maxLinkSortOrderOfProduct(@Param("id") Long id);

    @Query("select max(l.sortOrder) from NodeLink l where l.outcome.id = :id")
    Integer maxLinkSortOrderOfOutcome(@Param("id") Long id);

    @Query("select max(l.sortOrder) from NodeLink l where l.opportunity.id = :id")
    Integer maxLinkSortOrderOfOpportunity(@Param("id") Long id);

    @Query("select max(l.sortOrder) from NodeLink l where l.solution.id = :id")
    Integer maxLinkSortOrderOfSolution(@Param("id") Long id);

    @Query("select max(l.sortOrder) from NodeLink l where l.assumption.id = :id")
    Integer maxLinkSortOrderOfAssumption(@Param("id") Long id);

    @Query("select max(l.sortOrder) from NodeLink l where l.evidence.id = :id")
    Integer maxLinkSortOrderOfEvidence(@Param("id") Long id);

    @Query("select max(q.sortOrder) from OpenQuestion q where q.opportunity.id = :id")
    Integer maxQuestionSortOrderOfOpportunity(@Param("id") Long id);

    // --- comments (oldest first, author fetched) ----------------------------

    @Query("select c from Comment c join fetch c.author where c.outcome.id = :id order by c.createdDate asc, c.id asc")
    List<Comment> findCommentsOfOutcome(@Param("id") Long id);

    @Query("select c from Comment c join fetch c.author where c.opportunity.id = :id order by c.createdDate asc, c.id asc")
    List<Comment> findCommentsOfOpportunity(@Param("id") Long id);

    @Query("select c from Comment c join fetch c.author where c.solution.id = :id order by c.createdDate asc, c.id asc")
    List<Comment> findCommentsOfSolution(@Param("id") Long id);

    @Query("select c from Comment c join fetch c.author where c.assumption.id = :id order by c.createdDate asc, c.id asc")
    List<Comment> findCommentsOfAssumption(@Param("id") Long id);

    @Query("select c from Comment c join fetch c.author where c.evidence.id = :id order by c.createdDate asc, c.id asc")
    List<Comment> findCommentsOfEvidence(@Param("id") Long id);

    /** A single comment with its author loaded. */
    @Query("select c from Comment c join fetch c.author where c.id = :id")
    List<Comment> findCommentWithAuthor(@Param("id") Long id);

    // --- history (newest first, author fetched) -----------------------------

    @Query(
        "select h from NodeHistory h left join fetch h.author" +
            " where h.nodeType = :type and h.nodeId = :id order by h.createdDate desc, h.id desc"
    )
    List<NodeHistory> findHistoryOfNode(@Param("type") TreeNodeType type, @Param("id") Long id);
}
