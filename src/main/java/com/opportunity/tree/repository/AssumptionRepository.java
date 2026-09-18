package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Assumption;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Assumption entity.
 */
@Repository
public interface AssumptionRepository extends JpaRepository<Assumption, Long> {
    default Optional<Assumption> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Assumption> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Assumption> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select assumption from Assumption assumption left join fetch assumption.solution",
        countQuery = "select count(assumption) from Assumption assumption"
    )
    Page<Assumption> findAllWithToOneRelationships(Pageable pageable);

    @Query("select assumption from Assumption assumption left join fetch assumption.solution")
    List<Assumption> findAllWithToOneRelationships();

    @Query("select assumption from Assumption assumption left join fetch assumption.solution where assumption.id =:id")
    Optional<Assumption> findOneWithToOneRelationships(@Param("id") Long id);
}
