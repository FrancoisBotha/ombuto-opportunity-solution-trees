package com.opportunity.tree.repository;

import com.opportunity.tree.domain.SolutionLink;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the SolutionLink entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SolutionLinkRepository extends ReactiveCrudRepository<SolutionLink, Long>, SolutionLinkRepositoryInternal {
    @Override
    Mono<SolutionLink> findOneWithEagerRelationships(Long id);

    @Override
    Flux<SolutionLink> findAllWithEagerRelationships();

    @Override
    Flux<SolutionLink> findAllWithEagerRelationships(Pageable page);

    @Query("SELECT * FROM solution_link entity WHERE entity.solution_id = :id")
    Flux<SolutionLink> findBySolution(Long id);

    @Query("SELECT * FROM solution_link entity WHERE entity.solution_id IS NULL")
    Flux<SolutionLink> findAllWhereSolutionIsNull();

    @Override
    <S extends SolutionLink> Mono<S> save(S entity);

    @Override
    Flux<SolutionLink> findAll();

    @Override
    Mono<SolutionLink> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface SolutionLinkRepositoryInternal {
    <S extends SolutionLink> Mono<S> save(S entity);

    Flux<SolutionLink> findAllBy(Pageable pageable);

    Flux<SolutionLink> findAll();

    Mono<SolutionLink> findById(Long id);
    // this is not supported at the moment because of https://github.com/jhipster/generator-jhipster/issues/18269
    // Flux<SolutionLink> findAllBy(Pageable pageable, Criteria criteria);

    Mono<SolutionLink> findOneWithEagerRelationships(Long id);

    Flux<SolutionLink> findAllWithEagerRelationships();

    Flux<SolutionLink> findAllWithEagerRelationships(Pageable page);

    Mono<Void> deleteById(Long id);
}
