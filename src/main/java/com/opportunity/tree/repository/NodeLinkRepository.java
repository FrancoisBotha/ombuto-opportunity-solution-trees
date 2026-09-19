package com.opportunity.tree.repository;

import com.opportunity.tree.domain.NodeLink;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the NodeLink entity.
 */
@Repository
public interface NodeLinkRepository extends JpaRepository<NodeLink, Long> {
    default Optional<NodeLink> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<NodeLink> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<NodeLink> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select nodeLink from NodeLink nodeLink left join fetch nodeLink.product left join fetch nodeLink.outcome left join fetch nodeLink.opportunity left join fetch nodeLink.solution left join fetch nodeLink.assumption left join fetch nodeLink.evidence",
        countQuery = "select count(nodeLink) from NodeLink nodeLink"
    )
    Page<NodeLink> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select nodeLink from NodeLink nodeLink left join fetch nodeLink.product left join fetch nodeLink.outcome left join fetch nodeLink.opportunity left join fetch nodeLink.solution left join fetch nodeLink.assumption left join fetch nodeLink.evidence"
    )
    List<NodeLink> findAllWithToOneRelationships();

    @Query(
        "select nodeLink from NodeLink nodeLink left join fetch nodeLink.product left join fetch nodeLink.outcome left join fetch nodeLink.opportunity left join fetch nodeLink.solution left join fetch nodeLink.assumption left join fetch nodeLink.evidence where nodeLink.id =:id"
    )
    Optional<NodeLink> findOneWithToOneRelationships(@Param("id") Long id);
}
