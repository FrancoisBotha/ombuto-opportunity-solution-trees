package com.opportunity.tree.repository;

import com.opportunity.tree.domain.SolutionLink;
import com.opportunity.tree.repository.rowmapper.SolutionLinkRowMapper;
import com.opportunity.tree.repository.rowmapper.SolutionRowMapper;
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
 * Spring Data R2DBC custom repository implementation for the SolutionLink entity.
 */
@SuppressWarnings("unused")
class SolutionLinkRepositoryInternalImpl extends SimpleR2dbcRepository<SolutionLink, Long> implements SolutionLinkRepositoryInternal {

    private final DatabaseClient db;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final EntityManager entityManager;

    private final SolutionRowMapper solutionMapper;
    private final SolutionLinkRowMapper solutionlinkMapper;

    private static final Table entityTable = Table.aliased("solution_link", EntityManager.ENTITY_ALIAS);
    private static final Table solutionTable = Table.aliased("solution", "solution");

    public SolutionLinkRepositoryInternalImpl(
        R2dbcEntityTemplate template,
        EntityManager entityManager,
        SolutionRowMapper solutionMapper,
        SolutionLinkRowMapper solutionlinkMapper,
        R2dbcEntityOperations entityOperations,
        R2dbcConverter converter
    ) {
        super(
            new MappingRelationalEntityInformation(converter.getMappingContext().getRequiredPersistentEntity(SolutionLink.class)),
            entityOperations,
            converter
        );
        this.db = template.getDatabaseClient();
        this.r2dbcEntityTemplate = template;
        this.entityManager = entityManager;
        this.solutionMapper = solutionMapper;
        this.solutionlinkMapper = solutionlinkMapper;
    }

    @Override
    public Flux<SolutionLink> findAllBy(Pageable pageable) {
        return createQuery(pageable, null).all();
    }

    RowsFetchSpec<SolutionLink> createQuery(Pageable pageable, Condition whereClause) {
        List<Expression> columns = SolutionLinkSqlHelper.getColumns(entityTable, EntityManager.ENTITY_ALIAS);
        columns.addAll(SolutionSqlHelper.getColumns(solutionTable, "solution"));
        SelectFromAndJoinCondition selectFrom = Select.builder()
            .select(columns)
            .from(entityTable)
            .leftOuterJoin(solutionTable)
            .on(Column.create("solution_id", entityTable))
            .equals(Column.create("id", solutionTable));
        // we do not support Criteria here for now as of https://github.com/jhipster/generator-jhipster/issues/18269
        String select = entityManager.createSelect(selectFrom, SolutionLink.class, pageable, whereClause);
        return db.sql(select).map(this::process);
    }

    @Override
    public Flux<SolutionLink> findAll() {
        return findAllBy(null);
    }

    @Override
    public Mono<SolutionLink> findById(Long id) {
        Comparison whereClause = Conditions.isEqual(entityTable.column("id"), Conditions.just(id.toString()));
        return createQuery(null, whereClause).one();
    }

    @Override
    public Mono<SolutionLink> findOneWithEagerRelationships(Long id) {
        return findById(id);
    }

    @Override
    public Flux<SolutionLink> findAllWithEagerRelationships() {
        return findAll();
    }

    @Override
    public Flux<SolutionLink> findAllWithEagerRelationships(Pageable page) {
        return findAllBy(page);
    }

    private SolutionLink process(Row row, RowMetadata metadata) {
        SolutionLink entity = solutionlinkMapper.apply(row, "e");
        entity.setSolution(solutionMapper.apply(row, "solution"));
        return entity;
    }

    @Override
    public <S extends SolutionLink> Mono<S> save(S entity) {
        return super.save(entity);
    }
}
