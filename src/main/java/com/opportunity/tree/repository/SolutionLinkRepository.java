package com.opportunity.tree.repository;

import com.opportunity.tree.domain.SolutionLink;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the SolutionLink entity.
 */
@Repository
public interface SolutionLinkRepository extends JpaRepository<SolutionLink, Long> {
    default Optional<SolutionLink> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<SolutionLink> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<SolutionLink> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select solutionLink from SolutionLink solutionLink left join fetch solutionLink.solution",
        countQuery = "select count(solutionLink) from SolutionLink solutionLink"
    )
    Page<SolutionLink> findAllWithToOneRelationships(Pageable pageable);

    @Query("select solutionLink from SolutionLink solutionLink left join fetch solutionLink.solution")
    List<SolutionLink> findAllWithToOneRelationships();

    @Query("select solutionLink from SolutionLink solutionLink left join fetch solutionLink.solution where solutionLink.id =:id")
    Optional<SolutionLink> findOneWithToOneRelationships(@Param("id") Long id);
}
