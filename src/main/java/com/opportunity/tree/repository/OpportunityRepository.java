package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Opportunity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Opportunity entity.
 *
 * When extending this class, extend OpportunityRepositoryWithBagRelationships too.
 * For more information refer to https://github.com/jhipster/generator-jhipster/issues/17990.
 */
@Repository
public interface OpportunityRepository
    extends OpportunityRepositoryWithBagRelationships, JpaRepository<Opportunity, Long>, JpaSpecificationExecutor<Opportunity>
{
    @Query("select opportunity from Opportunity opportunity where opportunity.owner.login = ?#{authentication.name}")
    List<Opportunity> findByOwnerIsCurrentUser();

    default Optional<Opportunity> findOneWithEagerRelationships(Long id) {
        return this.fetchBagRelationships(this.findOneWithToOneRelationships(id));
    }

    default List<Opportunity> findAllWithEagerRelationships() {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships());
    }

    default Page<Opportunity> findAllWithEagerRelationships(Pageable pageable) {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships(pageable));
    }

    @Query(
        value = "select opportunity from Opportunity opportunity left join fetch opportunity.outcome left join fetch opportunity.parent left join fetch opportunity.owner",
        countQuery = "select count(opportunity) from Opportunity opportunity"
    )
    Page<Opportunity> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select opportunity from Opportunity opportunity left join fetch opportunity.outcome left join fetch opportunity.parent left join fetch opportunity.owner"
    )
    List<Opportunity> findAllWithToOneRelationships();

    @Query(
        "select opportunity from Opportunity opportunity left join fetch opportunity.outcome left join fetch opportunity.parent left join fetch opportunity.owner where opportunity.id =:id"
    )
    Optional<Opportunity> findOneWithToOneRelationships(@Param("id") Long id);
}
