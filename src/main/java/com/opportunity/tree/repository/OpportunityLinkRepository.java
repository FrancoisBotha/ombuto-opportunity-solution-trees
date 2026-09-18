package com.opportunity.tree.repository;

import com.opportunity.tree.domain.OpportunityLink;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the OpportunityLink entity.
 */
@Repository
public interface OpportunityLinkRepository extends JpaRepository<OpportunityLink, Long> {
    default Optional<OpportunityLink> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<OpportunityLink> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<OpportunityLink> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select opportunityLink from OpportunityLink opportunityLink left join fetch opportunityLink.opportunity",
        countQuery = "select count(opportunityLink) from OpportunityLink opportunityLink"
    )
    Page<OpportunityLink> findAllWithToOneRelationships(Pageable pageable);

    @Query("select opportunityLink from OpportunityLink opportunityLink left join fetch opportunityLink.opportunity")
    List<OpportunityLink> findAllWithToOneRelationships();

    @Query(
        "select opportunityLink from OpportunityLink opportunityLink left join fetch opportunityLink.opportunity where opportunityLink.id =:id"
    )
    Optional<OpportunityLink> findOneWithToOneRelationships(@Param("id") Long id);
}
