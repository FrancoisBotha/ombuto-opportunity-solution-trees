package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Evidence;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Evidence entity.
 */
@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, Long> {
    default Optional<Evidence> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Evidence> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Evidence> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select evidence from Evidence evidence left join fetch evidence.opportunity left join fetch evidence.assumption",
        countQuery = "select count(evidence) from Evidence evidence"
    )
    Page<Evidence> findAllWithToOneRelationships(Pageable pageable);

    @Query("select evidence from Evidence evidence left join fetch evidence.opportunity left join fetch evidence.assumption")
    List<Evidence> findAllWithToOneRelationships();

    @Query(
        "select evidence from Evidence evidence left join fetch evidence.opportunity left join fetch evidence.assumption where evidence.id =:id"
    )
    Optional<Evidence> findOneWithToOneRelationships(@Param("id") Long id);
}
