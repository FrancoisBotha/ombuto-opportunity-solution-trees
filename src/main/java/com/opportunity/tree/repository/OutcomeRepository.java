package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Outcome;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the Outcome entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OutcomeRepository extends ReactiveCrudRepository<Outcome, Long>, OutcomeRepositoryInternal {
    @Override
    Mono<Outcome> findOneWithEagerRelationships(Long id);

    @Override
    Flux<Outcome> findAllWithEagerRelationships();

    @Override
    Flux<Outcome> findAllWithEagerRelationships(Pageable page);

    @Query("SELECT * FROM outcome entity WHERE entity.product_id = :id")
    Flux<Outcome> findByProduct(Long id);

    @Query("SELECT * FROM outcome entity WHERE entity.product_id IS NULL")
    Flux<Outcome> findAllWhereProductIsNull();

    @Query("SELECT * FROM outcome entity WHERE entity.owner_id = :id")
    Flux<Outcome> findByOwner(Long id);

    @Query("SELECT * FROM outcome entity WHERE entity.owner_id IS NULL")
    Flux<Outcome> findAllWhereOwnerIsNull();

    @Override
    <S extends Outcome> Mono<S> save(S entity);

    @Override
    Flux<Outcome> findAll();

    @Override
    Mono<Outcome> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface OutcomeRepositoryInternal {
    <S extends Outcome> Mono<S> save(S entity);

    Flux<Outcome> findAllBy(Pageable pageable);

    Flux<Outcome> findAll();

    Mono<Outcome> findById(Long id);
    // this is not supported at the moment because of https://github.com/jhipster/generator-jhipster/issues/18269
    // Flux<Outcome> findAllBy(Pageable pageable, Criteria criteria);

    Mono<Outcome> findOneWithEagerRelationships(Long id);

    Flux<Outcome> findAllWithEagerRelationships();

    Flux<Outcome> findAllWithEagerRelationships(Pageable page);

    Mono<Void> deleteById(Long id);
}
