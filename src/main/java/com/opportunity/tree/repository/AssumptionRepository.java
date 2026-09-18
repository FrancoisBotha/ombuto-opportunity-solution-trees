package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Assumption;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the Assumption entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AssumptionRepository extends ReactiveCrudRepository<Assumption, Long>, AssumptionRepositoryInternal {
    @Override
    Mono<Assumption> findOneWithEagerRelationships(Long id);

    @Override
    Flux<Assumption> findAllWithEagerRelationships();

    @Override
    Flux<Assumption> findAllWithEagerRelationships(Pageable page);

    @Query("SELECT * FROM assumption entity WHERE entity.solution_id = :id")
    Flux<Assumption> findBySolution(Long id);

    @Query("SELECT * FROM assumption entity WHERE entity.solution_id IS NULL")
    Flux<Assumption> findAllWhereSolutionIsNull();

    @Override
    <S extends Assumption> Mono<S> save(S entity);

    @Override
    Flux<Assumption> findAll();

    @Override
    Mono<Assumption> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface AssumptionRepositoryInternal {
    <S extends Assumption> Mono<S> save(S entity);

    Flux<Assumption> findAllBy(Pageable pageable);

    Flux<Assumption> findAll();

    Mono<Assumption> findById(Long id);
    // this is not supported at the moment because of https://github.com/jhipster/generator-jhipster/issues/18269
    // Flux<Assumption> findAllBy(Pageable pageable, Criteria criteria);

    Mono<Assumption> findOneWithEagerRelationships(Long id);

    Flux<Assumption> findAllWithEagerRelationships();

    Flux<Assumption> findAllWithEagerRelationships(Pageable page);

    Mono<Void> deleteById(Long id);
}
