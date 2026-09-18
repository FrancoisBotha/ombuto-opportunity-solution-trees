package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Solution;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the Solution entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SolutionRepository extends ReactiveCrudRepository<Solution, Long>, SolutionRepositoryInternal {
    Flux<Solution> findAllBy(Pageable pageable);

    @Override
    Mono<Solution> findOneWithEagerRelationships(Long id);

    @Override
    Flux<Solution> findAllWithEagerRelationships();

    @Override
    Flux<Solution> findAllWithEagerRelationships(Pageable page);

    @Query("SELECT * FROM solution entity WHERE entity.opportunity_id = :id")
    Flux<Solution> findByOpportunity(Long id);

    @Query("SELECT * FROM solution entity WHERE entity.opportunity_id IS NULL")
    Flux<Solution> findAllWhereOpportunityIsNull();

    @Query("SELECT * FROM solution entity WHERE entity.owner_id = :id")
    Flux<Solution> findByOwner(Long id);

    @Query("SELECT * FROM solution entity WHERE entity.owner_id IS NULL")
    Flux<Solution> findAllWhereOwnerIsNull();

    @Query(
        "SELECT entity.* FROM solution entity JOIN rel_solution__tag joinTable ON entity.id = joinTable.tag_id WHERE joinTable.tag_id = :id"
    )
    Flux<Solution> findByTag(Long id);

    @Override
    <S extends Solution> Mono<S> save(S entity);

    @Override
    Flux<Solution> findAll();

    @Override
    Mono<Solution> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface SolutionRepositoryInternal {
    <S extends Solution> Mono<S> save(S entity);

    Flux<Solution> findAllBy(Pageable pageable);

    Flux<Solution> findAll();

    Mono<Solution> findById(Long id);
    // this is not supported at the moment because of https://github.com/jhipster/generator-jhipster/issues/18269
    // Flux<Solution> findAllBy(Pageable pageable, Criteria criteria);

    Mono<Solution> findOneWithEagerRelationships(Long id);

    Flux<Solution> findAllWithEagerRelationships();

    Flux<Solution> findAllWithEagerRelationships(Pageable page);

    Mono<Void> deleteById(Long id);
}
