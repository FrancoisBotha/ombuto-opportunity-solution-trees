package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Opportunity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the Opportunity entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OpportunityRepository extends ReactiveCrudRepository<Opportunity, Long>, OpportunityRepositoryInternal {
    Flux<Opportunity> findAllBy(Pageable pageable);

    @Override
    Mono<Opportunity> findOneWithEagerRelationships(Long id);

    @Override
    Flux<Opportunity> findAllWithEagerRelationships();

    @Override
    Flux<Opportunity> findAllWithEagerRelationships(Pageable page);

    @Query("SELECT * FROM opportunity entity WHERE entity.outcome_id = :id")
    Flux<Opportunity> findByOutcome(Long id);

    @Query("SELECT * FROM opportunity entity WHERE entity.outcome_id IS NULL")
    Flux<Opportunity> findAllWhereOutcomeIsNull();

    @Query("SELECT * FROM opportunity entity WHERE entity.parent_id = :id")
    Flux<Opportunity> findByParent(Long id);

    @Query("SELECT * FROM opportunity entity WHERE entity.parent_id IS NULL")
    Flux<Opportunity> findAllWhereParentIsNull();

    @Query("SELECT * FROM opportunity entity WHERE entity.owner_id = :id")
    Flux<Opportunity> findByOwner(Long id);

    @Query("SELECT * FROM opportunity entity WHERE entity.owner_id IS NULL")
    Flux<Opportunity> findAllWhereOwnerIsNull();

    @Query(
        "SELECT entity.* FROM opportunity entity JOIN rel_opportunity__interview joinTable ON entity.id = joinTable.interview_id WHERE joinTable.interview_id = :id"
    )
    Flux<Opportunity> findByInterview(Long id);

    @Query(
        "SELECT entity.* FROM opportunity entity JOIN rel_opportunity__tag joinTable ON entity.id = joinTable.tag_id WHERE joinTable.tag_id = :id"
    )
    Flux<Opportunity> findByTag(Long id);

    @Override
    <S extends Opportunity> Mono<S> save(S entity);

    @Override
    Flux<Opportunity> findAll();

    @Override
    Mono<Opportunity> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface OpportunityRepositoryInternal {
    <S extends Opportunity> Mono<S> save(S entity);

    Flux<Opportunity> findAllBy(Pageable pageable);

    Flux<Opportunity> findAll();

    Mono<Opportunity> findById(Long id);
    // this is not supported at the moment because of https://github.com/jhipster/generator-jhipster/issues/18269
    // Flux<Opportunity> findAllBy(Pageable pageable, Criteria criteria);

    Mono<Opportunity> findOneWithEagerRelationships(Long id);

    Flux<Opportunity> findAllWithEagerRelationships();

    Flux<Opportunity> findAllWithEagerRelationships(Pageable page);

    Mono<Void> deleteById(Long id);
}
