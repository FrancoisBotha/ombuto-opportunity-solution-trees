package com.opportunity.tree.repository;

import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.repository.rowmapper.TeamMemberRowMapper;
import com.opportunity.tree.repository.rowmapper.TeamRowMapper;
import com.opportunity.tree.repository.rowmapper.UserRowMapper;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.support.SimpleR2dbcRepository;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Comparison;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.SelectBuilder.SelectFromAndJoinCondition;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.repository.support.MappingRelationalEntityInformation;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.RowsFetchSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC custom repository implementation for the TeamMember entity.
 */
@SuppressWarnings("unused")
class TeamMemberRepositoryInternalImpl extends SimpleR2dbcRepository<TeamMember, Long> implements TeamMemberRepositoryInternal {

    private final DatabaseClient db;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final EntityManager entityManager;

    private final TeamRowMapper teamMapper;
    private final UserRowMapper userMapper;
    private final TeamMemberRowMapper teammemberMapper;

    private static final Table entityTable = Table.aliased("team_member", EntityManager.ENTITY_ALIAS);
    private static final Table teamTable = Table.aliased("team", "team");
    private static final Table userTable = Table.aliased("jhi_user", "e_user");

    public TeamMemberRepositoryInternalImpl(
        R2dbcEntityTemplate template,
        EntityManager entityManager,
        TeamRowMapper teamMapper,
        UserRowMapper userMapper,
        TeamMemberRowMapper teammemberMapper,
        R2dbcEntityOperations entityOperations,
        R2dbcConverter converter
    ) {
        super(
            new MappingRelationalEntityInformation(converter.getMappingContext().getRequiredPersistentEntity(TeamMember.class)),
            entityOperations,
            converter
        );
        this.db = template.getDatabaseClient();
        this.r2dbcEntityTemplate = template;
        this.entityManager = entityManager;
        this.teamMapper = teamMapper;
        this.userMapper = userMapper;
        this.teammemberMapper = teammemberMapper;
    }

    @Override
    public Flux<TeamMember> findAllBy(Pageable pageable) {
        return createQuery(pageable, null).all();
    }

    RowsFetchSpec<TeamMember> createQuery(Pageable pageable, Condition whereClause) {
        List<Expression> columns = TeamMemberSqlHelper.getColumns(entityTable, EntityManager.ENTITY_ALIAS);
        columns.addAll(TeamSqlHelper.getColumns(teamTable, "team"));
        columns.addAll(UserSqlHelper.getColumns(userTable, "user"));
        SelectFromAndJoinCondition selectFrom = Select.builder()
            .select(columns)
            .from(entityTable)
            .leftOuterJoin(teamTable)
            .on(Column.create("team_id", entityTable))
            .equals(Column.create("id", teamTable))
            .leftOuterJoin(userTable)
            .on(Column.create("user_id", entityTable))
            .equals(Column.create("id", userTable));
        // we do not support Criteria here for now as of https://github.com/jhipster/generator-jhipster/issues/18269
        String select = entityManager.createSelect(selectFrom, TeamMember.class, pageable, whereClause);
        return db.sql(select).map(this::process);
    }

    @Override
    public Flux<TeamMember> findAll() {
        return findAllBy(null);
    }

    @Override
    public Mono<TeamMember> findById(Long id) {
        Comparison whereClause = Conditions.isEqual(entityTable.column("id"), Conditions.just(id.toString()));
        return createQuery(null, whereClause).one();
    }

    @Override
    public Mono<TeamMember> findOneWithEagerRelationships(Long id) {
        return findById(id);
    }

    @Override
    public Flux<TeamMember> findAllWithEagerRelationships() {
        return findAll();
    }

    @Override
    public Flux<TeamMember> findAllWithEagerRelationships(Pageable page) {
        return findAllBy(page);
    }

    private TeamMember process(Row row, RowMetadata metadata) {
        TeamMember entity = teammemberMapper.apply(row, "e");
        entity.setTeam(teamMapper.apply(row, "team"));
        entity.setUser(userMapper.apply(row, "user"));
        return entity;
    }

    @Override
    public <S extends TeamMember> Mono<S> save(S entity) {
        return super.save(entity);
    }
}
