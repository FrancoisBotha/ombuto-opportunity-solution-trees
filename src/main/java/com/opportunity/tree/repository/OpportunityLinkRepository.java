package com.opportunity.tree.repository;

import com.opportunity.tree.domain.OpportunityLink;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the OpportunityLink entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OpportunityLinkRepository extends ReactiveCrudRepository<OpportunityLink, Long>, OpportunityLinkRepositoryInternal {
    @Override
    Mono<OpportunityLink> findOneWithEagerRelationships(Long id);

    @Override
    Flux<OpportunityLink> findAllWithEagerRelationships();

    @Override
    Flux<OpportunityLink> findAllWithEagerRelationships(Pageable page);

    @Query("SELECT * FROM opportunity_link entity WHERE entity.opportunity_id = :id")
    Flux<OpportunityLink> findByOpportunity(Long id);

    @Query("SELECT * FROM opportunity_link entity WHERE entity.opportunity_id IS NULL")
    Flux<OpportunityLink> findAllWhereOpportunityIsNull();

    @Override
    <S extends OpportunityLink> Mono<S> save(S entity);

    @Override
    Flux<OpportunityLink> findAll();

    @Override
    Mono<OpportunityLink> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface OpportunityLinkRepositoryInternal {
    <S extends OpportunityLink> Mono<S> save(S entity);

    Flux<OpportunityLink> findAllBy(Pageable pageable);

    Flux<OpportunityLink> findAll();

    Mono<OpportunityLink> findById(Long id);
    // this is not supported at the moment because of https://github.com/jhipster/generator-jhipster/issues/18269
    // Flux<OpportunityLink> findAllBy(Pageable pageable, Criteria criteria);

    Mono<OpportunityLink> findOneWithEagerRelationships(Long id);

    Flux<OpportunityLink> findAllWithEagerRelationships();

    Flux<OpportunityLink> findAllWithEagerRelationships(Pageable page);

    Mono<Void> deleteById(Long id);
}
