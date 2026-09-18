package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Interview;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Tag;
import com.opportunity.tree.repository.rowmapper.OpportunityRowMapper;
import com.opportunity.tree.repository.rowmapper.OpportunityRowMapper;
import com.opportunity.tree.repository.rowmapper.OutcomeRowMapper;
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
 * Spring Data R2DBC custom repository implementation for the Opportunity entity.
 */
@SuppressWarnings("unused")
class OpportunityRepositoryInternalImpl extends SimpleR2dbcRepository<Opportunity, Long> implements OpportunityRepositoryInternal {

    private final DatabaseClient db;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final EntityManager entityManager;

    private final OutcomeRowMapper outcomeMapper;
    private final OpportunityRowMapper opportunityMapper;
    private final UserRowMapper userMapper;

    private static final Table entityTable = Table.aliased("opportunity", EntityManager.ENTITY_ALIAS);
    private static final Table outcomeTable = Table.aliased("outcome", "outcome");
    private static final Table parentTable = Table.aliased("opportunity", "parent");
    private static final Table ownerTable = Table.aliased("jhi_user", "owner");

    private static final EntityManager.LinkTable interviewLink = new EntityManager.LinkTable(
        "rel_opportunity__interview",
        "opportunity_id",
        "interview_id"
    );
    private static final EntityManager.LinkTable tagLink = new EntityManager.LinkTable("rel_opportunity__tag", "opportunity_id", "tag_id");

    public OpportunityRepositoryInternalImpl(
        R2dbcEntityTemplate template,
        EntityManager entityManager,
        OutcomeRowMapper outcomeMapper,
        OpportunityRowMapper opportunityMapper,
        UserRowMapper userMapper,
        R2dbcEntityOperations entityOperations,
        R2dbcConverter converter
    ) {
        super(
            new MappingRelationalEntityInformation(converter.getMappingContext().getRequiredPersistentEntity(Opportunity.class)),
            entityOperations,
            converter
        );
        this.db = template.getDatabaseClient();
        this.r2dbcEntityTemplate = template;
        this.entityManager = entityManager;
        this.outcomeMapper = outcomeMapper;
        this.opportunityMapper = opportunityMapper;
        this.userMapper = userMapper;
    }

    @Override
    public Flux<Opportunity> findAllBy(Pageable pageable) {
        return createQuery(pageable, null).all();
    }

    RowsFetchSpec<Opportunity> createQuery(Pageable pageable, Condition whereClause) {
        List<Expression> columns = OpportunitySqlHelper.getColumns(entityTable, EntityManager.ENTITY_ALIAS);
        columns.addAll(OutcomeSqlHelper.getColumns(outcomeTable, "outcome"));
        columns.addAll(OpportunitySqlHelper.getColumns(parentTable, "parent"));
        columns.addAll(UserSqlHelper.getColumns(ownerTable, "owner"));
        SelectFromAndJoinCondition selectFrom = Select.builder()
            .select(columns)
            .from(entityTable)
            .leftOuterJoin(outcomeTable)
            .on(Column.create("outcome_id", entityTable))
            .equals(Column.create("id", outcomeTable))
            .leftOuterJoin(parentTable)
            .on(Column.create("parent_id", entityTable))
            .equals(Column.create("id", parentTable))
            .leftOuterJoin(ownerTable)
            .on(Column.create("owner_id", entityTable))
            .equals(Column.create("id", ownerTable));
        // we do not support Criteria here for now as of https://github.com/jhipster/generator-jhipster/issues/18269
        String select = entityManager.createSelect(selectFrom, Opportunity.class, pageable, whereClause);
        return db.sql(select).map(this::process);
    }

    @Override
    public Flux<Opportunity> findAll() {
        return findAllBy(null);
    }

    @Override
    public Mono<Opportunity> findById(Long id) {
        Comparison whereClause = Conditions.isEqual(entityTable.column("id"), Conditions.just(id.toString()));
        return createQuery(null, whereClause).one();
    }

    @Override
    public Mono<Opportunity> findOneWithEagerRelationships(Long id) {
        return findById(id);
    }

    @Override
    public Flux<Opportunity> findAllWithEagerRelationships() {
        return findAll();
    }

    @Override
    public Flux<Opportunity> findAllWithEagerRelationships(Pageable page) {
        return findAllBy(page);
    }

    private Opportunity process(Row row, RowMetadata metadata) {
        Opportunity entity = opportunityMapper.apply(row, "e");
        entity.setOutcome(outcomeMapper.apply(row, "outcome"));
        entity.setParent(opportunityMapper.apply(row, "parent"));
        entity.setOwner(userMapper.apply(row, "owner"));
        return entity;
    }

    @Override
    public <S extends Opportunity> Mono<S> save(S entity) {
        return super.save(entity).flatMap((S e) -> updateRelations(e));
    }

    protected <S extends Opportunity> Mono<S> updateRelations(S entity) {
        Mono<Void> result = entityManager
            .updateLinkTable(interviewLink, entity.getId(), entity.getInterviews().stream().map(Interview::getId))
            .then();
        result = result.and(entityManager.updateLinkTable(tagLink, entity.getId(), entity.getTags().stream().map(Tag::getId)));
        return result.thenReturn(entity);
    }

    @Override
    public Mono<Void> deleteById(Long entityId) {
        return deleteRelations(entityId).then(super.deleteById(entityId));
    }

    protected Mono<Void> deleteRelations(Long entityId) {
        return entityManager.deleteFromLinkTable(interviewLink, entityId).and(entityManager.deleteFromLinkTable(tagLink, entityId));
    }
}
