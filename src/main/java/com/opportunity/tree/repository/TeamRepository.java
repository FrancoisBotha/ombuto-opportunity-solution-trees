package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Team;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the Team entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TeamRepository extends ReactiveCrudRepository<Team, Long>, TeamRepositoryInternal {
    @Override
    <S extends Team> Mono<S> save(S entity);

    @Override
    Flux<Team> findAll();

    @Override
    Mono<Team> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface TeamRepositoryInternal {
    <S extends Team> Mono<S> save(S entity);

    Flux<Team> findAllBy(Pageable pageable);

    Flux<Team> findAll();

    Mono<Team> findById(Long id);
    // this is not supported at the moment because of https://github.com/jhipster/generator-jhipster/issues/18269
    // Flux<Team> findAllBy(Pageable pageable, Criteria criteria);
}
