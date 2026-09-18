package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Interview;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the Interview entity.
 */
@SuppressWarnings("unused")
@Repository
public interface InterviewRepository extends ReactiveCrudRepository<Interview, Long>, InterviewRepositoryInternal {
    Flux<Interview> findAllBy(Pageable pageable);

    @Override
    Mono<Interview> findOneWithEagerRelationships(Long id);

    @Override
    Flux<Interview> findAllWithEagerRelationships();

    @Override
    Flux<Interview> findAllWithEagerRelationships(Pageable page);

    @Query("SELECT * FROM interview entity WHERE entity.product_id = :id")
    Flux<Interview> findByProduct(Long id);

    @Query("SELECT * FROM interview entity WHERE entity.product_id IS NULL")
    Flux<Interview> findAllWhereProductIsNull();

    @Query("SELECT * FROM interview entity WHERE entity.interviewer_id = :id")
    Flux<Interview> findByInterviewer(Long id);

    @Query("SELECT * FROM interview entity WHERE entity.interviewer_id IS NULL")
    Flux<Interview> findAllWhereInterviewerIsNull();

    @Override
    <S extends Interview> Mono<S> save(S entity);

    @Override
    Flux<Interview> findAll();

    @Override
    Mono<Interview> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface InterviewRepositoryInternal {
    <S extends Interview> Mono<S> save(S entity);

    Flux<Interview> findAllBy(Pageable pageable);

    Flux<Interview> findAll();

    Mono<Interview> findById(Long id);
    // this is not supported at the moment because of https://github.com/jhipster/generator-jhipster/issues/18269
    // Flux<Interview> findAllBy(Pageable pageable, Criteria criteria);

    Mono<Interview> findOneWithEagerRelationships(Long id);

    Flux<Interview> findAllWithEagerRelationships();

    Flux<Interview> findAllWithEagerRelationships(Pageable page);

    Mono<Void> deleteById(Long id);
}
