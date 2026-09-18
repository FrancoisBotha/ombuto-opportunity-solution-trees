package com.opportunity.tree.repository;

import com.opportunity.tree.domain.TeamMember;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the TeamMember entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TeamMemberRepository extends ReactiveCrudRepository<TeamMember, Long>, TeamMemberRepositoryInternal {
    @Override
    Mono<TeamMember> findOneWithEagerRelationships(Long id);

    @Override
    Flux<TeamMember> findAllWithEagerRelationships();

    @Override
    Flux<TeamMember> findAllWithEagerRelationships(Pageable page);

    @Query("SELECT * FROM team_member entity WHERE entity.team_id = :id")
    Flux<TeamMember> findByTeam(Long id);

    @Query("SELECT * FROM team_member entity WHERE entity.team_id IS NULL")
    Flux<TeamMember> findAllWhereTeamIsNull();

    @Query("SELECT * FROM team_member entity WHERE entity.user_id = :id")
    Flux<TeamMember> findByUser(Long id);

    @Query("SELECT * FROM team_member entity WHERE entity.user_id IS NULL")
    Flux<TeamMember> findAllWhereUserIsNull();

    @Override
    <S extends TeamMember> Mono<S> save(S entity);

    @Override
    Flux<TeamMember> findAll();

    @Override
    Mono<TeamMember> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface TeamMemberRepositoryInternal {
    <S extends TeamMember> Mono<S> save(S entity);

    Flux<TeamMember> findAllBy(Pageable pageable);

    Flux<TeamMember> findAll();

    Mono<TeamMember> findById(Long id);
    // this is not supported at the moment because of https://github.com/jhipster/generator-jhipster/issues/18269
    // Flux<TeamMember> findAllBy(Pageable pageable, Criteria criteria);

    Mono<TeamMember> findOneWithEagerRelationships(Long id);

    Flux<TeamMember> findAllWithEagerRelationships();

    Flux<TeamMember> findAllWithEagerRelationships(Pageable page);

    Mono<Void> deleteById(Long id);
}
