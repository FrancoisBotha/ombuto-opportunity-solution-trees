package com.opportunity.tree.repository;

import com.opportunity.tree.domain.OpportunityLink;
import com.opportunity.tree.repository.rowmapper.OpportunityLinkRowMapper;
import com.opportunity.tree.repository.rowmapper.OpportunityRowMapper;
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
 * Spring Data R2DBC custom repository implementation for the OpportunityLink entity.
 */
@SuppressWarnings("unused")
class OpportunityLinkRepositoryInternalImpl
    extends SimpleR2dbcRepository<OpportunityLink, Long>
    implements OpportunityLinkRepositoryInternal
{

    private final DatabaseClient db;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final EntityManager entityManager;

    private final OpportunityRowMapper opportunityMapper;
    private final OpportunityLinkRowMapper opportunitylinkMapper;

    private static final Table entityTable = Table.aliased("opportunity_link", EntityManager.ENTITY_ALIAS);
    private static final Table opportunityTable = Table.aliased("opportunity", "opportunity");

    public OpportunityLinkRepositoryInternalImpl(
        R2dbcEntityTemplate template,
        EntityManager entityManager,
        OpportunityRowMapper opportunityMapper,
        OpportunityLinkRowMapper opportunitylinkMapper,
        R2dbcEntityOperations entityOperations,
        R2dbcConverter converter
    ) {
        super(
            new MappingRelationalEntityInformation(converter.getMappingContext().getRequiredPersistentEntity(OpportunityLink.class)),
            entityOperations,
            converter
        );
        this.db = template.getDatabaseClient();
        this.r2dbcEntityTemplate = template;
        this.entityManager = entityManager;
        this.opportunityMapper = opportunityMapper;
        this.opportunitylinkMapper = opportunitylinkMapper;
    }

    @Override
    public Flux<OpportunityLink> findAllBy(Pageable pageable) {
        return createQuery(pageable, null).all();
    }

    RowsFetchSpec<OpportunityLink> createQuery(Pageable pageable, Condition whereClause) {
        List<Expression> columns = OpportunityLinkSqlHelper.getColumns(entityTable, EntityManager.ENTITY_ALIAS);
        columns.addAll(OpportunitySqlHelper.getColumns(opportunityTable, "opportunity"));
        SelectFromAndJoinCondition selectFrom = Select.builder()
            .select(columns)
            .from(entityTable)
            .leftOuterJoin(opportunityTable)
            .on(Column.create("opportunity_id", entityTable))
            .equals(Column.create("id", opportunityTable));
        // we do not support Criteria here for now as of https://github.com/jhipster/generator-jhipster/issues/18269
        String select = entityManager.createSelect(selectFrom, OpportunityLink.class, pageable, whereClause);
        return db.sql(select).map(this::process);
    }

    @Override
    public Flux<OpportunityLink> findAll() {
        return findAllBy(null);
    }

    @Override
    public Mono<OpportunityLink> findById(Long id) {
        Comparison whereClause = Conditions.isEqual(entityTable.column("id"), Conditions.just(id.toString()));
        return createQuery(null, whereClause).one();
    }

    @Override
    public Mono<OpportunityLink> findOneWithEagerRelationships(Long id) {
        return findById(id);
    }

    @Override
    public Flux<OpportunityLink> findAllWithEagerRelationships() {
        return findAll();
    }

    @Override
    public Flux<OpportunityLink> findAllWithEagerRelationships(Pageable page) {
        return findAllBy(page);
    }

    private OpportunityLink process(Row row, RowMetadata metadata) {
        OpportunityLink entity = opportunitylinkMapper.apply(row, "e");
        entity.setOpportunity(opportunityMapper.apply(row, "opportunity"));
        return entity;
    }

    @Override
    public <S extends OpportunityLink> Mono<S> save(S entity) {
        return super.save(entity);
    }
}
