package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.repository.rowmapper.AssumptionRowMapper;
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
 * Spring Data R2DBC custom repository implementation for the Assumption entity.
 */
@SuppressWarnings("unused")
class AssumptionRepositoryInternalImpl extends SimpleR2dbcRepository<Assumption, Long> implements AssumptionRepositoryInternal {

    private final DatabaseClient db;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final EntityManager entityManager;

    private final SolutionRowMapper solutionMapper;
    private final AssumptionRowMapper assumptionMapper;

    private static final Table entityTable = Table.aliased("assumption", EntityManager.ENTITY_ALIAS);
    private static final Table solutionTable = Table.aliased("solution", "solution");

    public AssumptionRepositoryInternalImpl(
        R2dbcEntityTemplate template,
        EntityManager entityManager,
        SolutionRowMapper solutionMapper,
        AssumptionRowMapper assumptionMapper,
        R2dbcEntityOperations entityOperations,
        R2dbcConverter converter
    ) {
        super(
            new MappingRelationalEntityInformation(converter.getMappingContext().getRequiredPersistentEntity(Assumption.class)),
            entityOperations,
            converter
        );
        this.db = template.getDatabaseClient();
        this.r2dbcEntityTemplate = template;
        this.entityManager = entityManager;
        this.solutionMapper = solutionMapper;
        this.assumptionMapper = assumptionMapper;
    }

    @Override
    public Flux<Assumption> findAllBy(Pageable pageable) {
        return createQuery(pageable, null).all();
    }

    RowsFetchSpec<Assumption> createQuery(Pageable pageable, Condition whereClause) {
        List<Expression> columns = AssumptionSqlHelper.getColumns(entityTable, EntityManager.ENTITY_ALIAS);
        columns.addAll(SolutionSqlHelper.getColumns(solutionTable, "solution"));
        SelectFromAndJoinCondition selectFrom = Select.builder()
            .select(columns)
            .from(entityTable)
            .leftOuterJoin(solutionTable)
            .on(Column.create("solution_id", entityTable))
            .equals(Column.create("id", solutionTable));
        // we do not support Criteria here for now as of https://github.com/jhipster/generator-jhipster/issues/18269
        String select = entityManager.createSelect(selectFrom, Assumption.class, pageable, whereClause);
        return db.sql(select).map(this::process);
    }

    @Override
    public Flux<Assumption> findAll() {
        return findAllBy(null);
    }

    @Override
    public Mono<Assumption> findById(Long id) {
        Comparison whereClause = Conditions.isEqual(entityTable.column("id"), Conditions.just(id.toString()));
        return createQuery(null, whereClause).one();
    }

    @Override
    public Mono<Assumption> findOneWithEagerRelationships(Long id) {
        return findById(id);
    }

    @Override
    public Flux<Assumption> findAllWithEagerRelationships() {
        return findAll();
    }

    @Override
    public Flux<Assumption> findAllWithEagerRelationships(Pageable page) {
        return findAllBy(page);
    }

    private Assumption process(Row row, RowMetadata metadata) {
        Assumption entity = assumptionMapper.apply(row, "e");
        entity.setSolution(solutionMapper.apply(row, "solution"));
        return entity;
    }

    @Override
    public <S extends Assumption> Mono<S> save(S entity) {
        return super.save(entity);
    }
}
