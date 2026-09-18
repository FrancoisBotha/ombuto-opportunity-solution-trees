package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the Tag entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TagRepository extends ReactiveCrudRepository<Tag, Long>, TagRepositoryInternal {
    @Override
    Mono<Tag> findOneWithEagerRelationships(Long id);

    @Override
    Flux<Tag> findAllWithEagerRelationships();

    @Override
    Flux<Tag> findAllWithEagerRelationships(Pageable page);

    @Query("SELECT * FROM tag entity WHERE entity.team_id = :id")
    Flux<Tag> findByTeam(Long id);

    @Query("SELECT * FROM tag entity WHERE entity.team_id IS NULL")
    Flux<Tag> findAllWhereTeamIsNull();

    @Override
    <S extends Tag> Mono<S> save(S entity);

    @Override
    Flux<Tag> findAll();

    @Override
    Mono<Tag> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface TagRepositoryInternal {
    <S extends Tag> Mono<S> save(S entity);

    Flux<Tag> findAllBy(Pageable pageable);

    Flux<Tag> findAll();

    Mono<Tag> findById(Long id);
    // this is not supported at the moment because of https://github.com/jhipster/generator-jhipster/issues/18269
    // Flux<Tag> findAllBy(Pageable pageable, Criteria criteria);

    Mono<Tag> findOneWithEagerRelationships(Long id);

    Flux<Tag> findAllWithEagerRelationships();

    Flux<Tag> findAllWithEagerRelationships(Pageable page);

    Mono<Void> deleteById(Long id);
}
