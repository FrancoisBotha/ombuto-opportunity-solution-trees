package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Outcome;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Outcome entity.
 */
@Repository
public interface OutcomeRepository extends JpaRepository<Outcome, Long>, JpaSpecificationExecutor<Outcome> {
    @Query("select outcome from Outcome outcome where outcome.owner.login = ?#{authentication.name}")
    List<Outcome> findByOwnerIsCurrentUser();

    default Optional<Outcome> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Outcome> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Outcome> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select outcome from Outcome outcome left join fetch outcome.product left join fetch outcome.owner",
        countQuery = "select count(outcome) from Outcome outcome"
    )
    Page<Outcome> findAllWithToOneRelationships(Pageable pageable);

    @Query("select outcome from Outcome outcome left join fetch outcome.product left join fetch outcome.owner")
    List<Outcome> findAllWithToOneRelationships();

    @Query("select outcome from Outcome outcome left join fetch outcome.product left join fetch outcome.owner where outcome.id =:id")
    Optional<Outcome> findOneWithToOneRelationships(@Param("id") Long id);

    @Query("select coalesce(max(o.sortOrder), -1) from Outcome o where o.product.id = :productId")
    Integer findMaxSortOrderByProductId(@Param("productId") Long productId);
}
