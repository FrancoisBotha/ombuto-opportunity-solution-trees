package com.opportunity.tree.service;

import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.NodeLink;
import com.opportunity.tree.domain.OpenQuestion;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.service.dto.tree.TreeNodeDTO;
import com.opportunity.tree.service.dto.tree.TreeNodeLinkDTO;
import com.opportunity.tree.service.dto.tree.TreeOpenQuestionDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the {@link TreeNodeDTO} of a single node for the write endpoints, in exactly the shape
 * the flat tree read ({@code GET /api/teams/{teamId}/tree}) uses, so the client can splice a
 * write response straight into its node list. {@code lastActivity} is only computed by the tree
 * read and is left {@code null} here.
 */
@Component
@Transactional(readOnly = true)
public class TreeNodeDtoAssembler {

    @PersistenceContext
    private EntityManager em;

    /** Loads the node and maps it; the caller has already checked access and that the node exists. */
    public TreeNodeDTO toDto(TreeNodeType type, Long id) {
        TreeNodeDTO n = new TreeNodeDTO();
        n.setKey(TreeNodeRef.key(type, id));
        n.setType(type);
        n.setId(id);
        switch (type) {
            case PRODUCT -> {
                Product p = em.find(Product.class, id);
                n.setTitle(p.getName());
                n.setNotes(p.getDescription());
                n.setSortOrder(p.getSortOrder());
                n.setArchived(p.getArchived());
                n.setCreatedDate(p.getCreatedDate());
            }
            case OUTCOME -> {
                Outcome o = em.find(Outcome.class, id);
                n.setParentKey(TreeNodeRef.key(TreeNodeType.PRODUCT, o.getProduct().getId()));
                common(n, o.getTitle(), o.getDescription(), o.getSortOrder(), o.getCreatedDate(), o.getLastModifiedDate(), o.getOwner());
            }
            case OPPORTUNITY -> {
                Opportunity o = em.find(Opportunity.class, id);
                n.setParentKey(
                    o.getParent() != null
                        ? TreeNodeRef.key(TreeNodeType.OPPORTUNITY, o.getParent().getId())
                        : TreeNodeRef.key(TreeNodeType.OUTCOME, o.getOutcome().getId())
                );
                common(n, o.getTitle(), o.getDescription(), o.getSortOrder(), o.getCreatedDate(), o.getLastModifiedDate(), o.getOwner());
                n.setStatus(o.getStatus() == null ? null : o.getStatus().name());
                n.setPriority(o.getPriority());
                n.setValueRating(o.getValuerating());
                for (OpenQuestion q : em
                    .createQuery(
                        "select q from OpenQuestion q where q.opportunity.id = :id order by q.sortOrder asc, q.id asc",
                        OpenQuestion.class
                    )
                    .setParameter("id", id)
                    .getResultList()) {
                    n.getQuestions().add(new TreeOpenQuestionDTO(q.getId(), q.getQuestionText(), q.getDone()));
                }
            }
            case SOLUTION -> {
                Solution s = em.find(Solution.class, id);
                n.setParentKey(TreeNodeRef.key(TreeNodeType.OPPORTUNITY, s.getOpportunity().getId()));
                common(n, s.getTitle(), s.getDescription(), s.getSortOrder(), s.getCreatedDate(), s.getLastModifiedDate(), s.getOwner());
                n.setStatus(s.getStatus() == null ? null : s.getStatus().name());
            }
            case ASSUMPTION -> {
                Assumption a = em.find(Assumption.class, id);
                n.setParentKey(TreeNodeRef.key(TreeNodeType.SOLUTION, a.getSolution().getId()));
                common(
                    n,
                    a.getStatement(),
                    a.getDescription(),
                    a.getSortOrder(),
                    a.getCreatedDate(),
                    a.getLastModifiedDate(),
                    a.getOwner()
                );
                n.setStatus(a.getStatus() == null ? null : a.getStatus().name());
                n.setConfidence(a.getConfidence());
            }
            case EVIDENCE -> {
                Evidence e = em.find(Evidence.class, id);
                n.setParentKey(
                    e.getOpportunity() != null
                        ? TreeNodeRef.key(TreeNodeType.OPPORTUNITY, e.getOpportunity().getId())
                        : e.getAssumption() != null
                            ? TreeNodeRef.key(TreeNodeType.ASSUMPTION, e.getAssumption().getId())
                            : null
                );
                common(n, e.getTitle(), e.getDescription(), e.getSortOrder(), e.getCreatedDate(), e.getLastModifiedDate(), null);
            }
        }
        String fk = TreeNodeRules.label(type);
        for (NodeLink l : em
            .createQuery("select l from NodeLink l where l." + fk + ".id = :id order by l.sortOrder asc, l.id asc", NodeLink.class)
            .setParameter("id", id)
            .getResultList()) {
            n.getLinks().add(new TreeNodeLinkDTO(l.getId(), l.getName(), l.getUrl()));
        }
        if (type != TreeNodeType.PRODUCT) {
            Long count = em
                .createQuery("select count(c) from Comment c where c." + fk + ".id = :id", Long.class)
                .setParameter("id", id)
                .getSingleResult();
            n.setCommentCount(count == null ? 0 : count);
        }
        return n;
    }

    private static void common(
        TreeNodeDTO n,
        String title,
        String notes,
        Integer sortOrder,
        java.time.Instant created,
        java.time.Instant lastModified,
        User owner
    ) {
        n.setTitle(title);
        n.setNotes(notes);
        n.setSortOrder(sortOrder);
        n.setCreatedDate(created);
        n.setLastModifiedDate(lastModified);
        n.setOwnerLogin(owner == null ? null : owner.getLogin());
    }
}
