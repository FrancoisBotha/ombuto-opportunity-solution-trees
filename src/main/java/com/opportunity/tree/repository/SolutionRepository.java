package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Solution;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Solution entity.
 *
 * When extending this class, extend SolutionRepositoryWithBagRelationships too.
 * For more information refer to https://github.com/jhipster/generator-jhipster/issues/17990.
 */
@Repository
public interface SolutionRepository
    extends SolutionRepositoryWithBagRelationships, JpaRepository<Solution, Long>, JpaSpecificationExecutor<Solution>
{
    @Query("select solution from Solution solution where solution.owner.login = ?#{authentication.name}")
    List<Solution> findByOwnerIsCurrentUser();

    default Optional<Solution> findOneWithEagerRelationships(Long id) {
        return this.fetchBagRelationships(this.findOneWithToOneRelationships(id));
    }

    default List<Solution> findAllWithEagerRelationships() {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships());
    }

    default Page<Solution> findAllWithEagerRelationships(Pageable pageable) {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships(pageable));
    }

    @Query(
        value = "select solution from Solution solution left join fetch solution.opportunity left join fetch solution.owner",
        countQuery = "select count(solution) from Solution solution"
    )
    Page<Solution> findAllWithToOneRelationships(Pageable pageable);

    @Query("select solution from Solution solution left join fetch solution.opportunity left join fetch solution.owner")
    List<Solution> findAllWithToOneRelationships();

    @Query(
        "select solution from Solution solution left join fetch solution.opportunity left join fetch solution.owner where solution.id =:id"
    )
    Optional<Solution> findOneWithToOneRelationships(@Param("id") Long id);
}
