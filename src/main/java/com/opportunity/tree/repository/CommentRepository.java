package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the Comment entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CommentRepository extends ReactiveCrudRepository<Comment, Long>, CommentRepositoryInternal {
    Flux<Comment> findAllBy(Pageable pageable);

    @Override
    Mono<Comment> findOneWithEagerRelationships(Long id);

    @Override
    Flux<Comment> findAllWithEagerRelationships();

    @Override
    Flux<Comment> findAllWithEagerRelationships(Pageable page);

    @Query("SELECT * FROM comment entity WHERE entity.author_id = :id")
    Flux<Comment> findByAuthor(Long id);

    @Query("SELECT * FROM comment entity WHERE entity.author_id IS NULL")
    Flux<Comment> findAllWhereAuthorIsNull();

    @Query("SELECT * FROM comment entity WHERE entity.parent_id = :id")
    Flux<Comment> findByParent(Long id);

    @Query("SELECT * FROM comment entity WHERE entity.parent_id IS NULL")
    Flux<Comment> findAllWhereParentIsNull();

    @Query("SELECT * FROM comment entity WHERE entity.outcome_id = :id")
    Flux<Comment> findByOutcome(Long id);

    @Query("SELECT * FROM comment entity WHERE entity.outcome_id IS NULL")
    Flux<Comment> findAllWhereOutcomeIsNull();

    @Query("SELECT * FROM comment entity WHERE entity.opportunity_id = :id")
    Flux<Comment> findByOpportunity(Long id);

    @Query("SELECT * FROM comment entity WHERE entity.opportunity_id IS NULL")
    Flux<Comment> findAllWhereOpportunityIsNull();

    @Query("SELECT * FROM comment entity WHERE entity.solution_id = :id")
    Flux<Comment> findBySolution(Long id);

    @Query("SELECT * FROM comment entity WHERE entity.solution_id IS NULL")
    Flux<Comment> findAllWhereSolutionIsNull();

    @Override
    <S extends Comment> Mono<S> save(S entity);

    @Override
    Flux<Comment> findAll();

    @Override
    Mono<Comment> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface CommentRepositoryInternal {
    <S extends Comment> Mono<S> save(S entity);

    Flux<Comment> findAllBy(Pageable pageable);

    Flux<Comment> findAll();

    Mono<Comment> findById(Long id);
    // this is not supported at the moment because of https://github.com/jhipster/generator-jhipster/issues/18269
    // Flux<Comment> findAllBy(Pageable pageable, Criteria criteria);

    Mono<Comment> findOneWithEagerRelationships(Long id);

    Flux<Comment> findAllWithEagerRelationships();

    Flux<Comment> findAllWithEagerRelationships(Pageable page);

    Mono<Void> deleteById(Long id);
}
