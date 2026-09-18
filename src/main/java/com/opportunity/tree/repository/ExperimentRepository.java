package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Experiment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the Experiment entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ExperimentRepository extends ReactiveCrudRepository<Experiment, Long>, ExperimentRepositoryInternal {
    Flux<Experiment> findAllBy(Pageable pageable);

    @Override
    Mono<Experiment> findOneWithEagerRelationships(Long id);

    @Override
    Flux<Experiment> findAllWithEagerRelationships();

    @Override
    Flux<Experiment> findAllWithEagerRelationships(Pageable page);

    @Query("SELECT * FROM experiment entity WHERE entity.solution_id = :id")
    Flux<Experiment> findBySolution(Long id);

    @Query("SELECT * FROM experiment entity WHERE entity.solution_id IS NULL")
    Flux<Experiment> findAllWhereSolutionIsNull();

    @Query(
        "SELECT entity.* FROM experiment entity JOIN rel_experiment__assumption joinTable ON entity.id = joinTable.assumption_id WHERE joinTable.assumption_id = :id"
    )
    Flux<Experiment> findByAssumption(Long id);

    @Override
    <S extends Experiment> Mono<S> save(S entity);

    @Override
    Flux<Experiment> findAll();

    @Override
    Mono<Experiment> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface ExperimentRepositoryInternal {
    <S extends Experiment> Mono<S> save(S entity);

    Flux<Experiment> findAllBy(Pageable pageable);

    Flux<Experiment> findAll();

    Mono<Experiment> findById(Long id);
    // this is not supported at the moment because of https://github.com/jhipster/generator-jhipster/issues/18269
    // Flux<Experiment> findAllBy(Pageable pageable, Criteria criteria);

    Mono<Experiment> findOneWithEagerRelationships(Long id);

    Flux<Experiment> findAllWithEagerRelationships();

    Flux<Experiment> findAllWithEagerRelationships(Pageable page);

    Mono<Void> deleteById(Long id);
}
