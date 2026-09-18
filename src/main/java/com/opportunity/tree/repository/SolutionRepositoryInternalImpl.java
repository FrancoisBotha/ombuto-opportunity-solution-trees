package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Tag;
import com.opportunity.tree.repository.rowmapper.OpportunityRowMapper;
import com.opportunity.tree.repository.rowmapper.SolutionRowMapper;
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
 * Spring Data R2DBC custom repository implementation for the Solution entity.
 */
@SuppressWarnings("unused")
class SolutionRepositoryInternalImpl extends SimpleR2dbcRepository<Solution, Long> implements SolutionRepositoryInternal {

    private final DatabaseClient db;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final EntityManager entityManager;

    private final OpportunityRowMapper opportunityMapper;
    private final UserRowMapper userMapper;
    private final SolutionRowMapper solutionMapper;

    private static final Table entityTable = Table.aliased("solution", EntityManager.ENTITY_ALIAS);
    private static final Table opportunityTable = Table.aliased("opportunity", "opportunity");
    private static final Table ownerTable = Table.aliased("jhi_user", "owner");

    private static final EntityManager.LinkTable tagLink = new EntityManager.LinkTable("rel_solution__tag", "solution_id", "tag_id");

    public SolutionRepositoryInternalImpl(
        R2dbcEntityTemplate template,
        EntityManager entityManager,
        OpportunityRowMapper opportunityMapper,
        UserRowMapper userMapper,
        SolutionRowMapper solutionMapper,
        R2dbcEntityOperations entityOperations,
        R2dbcConverter converter
    ) {
        super(
            new MappingRelationalEntityInformation(converter.getMappingContext().getRequiredPersistentEntity(Solution.class)),
            entityOperations,
            converter
        );
        this.db = template.getDatabaseClient();
        this.r2dbcEntityTemplate = template;
        this.entityManager = entityManager;
        this.opportunityMapper = opportunityMapper;
        this.userMapper = userMapper;
        this.solutionMapper = solutionMapper;
    }

    @Override
    public Flux<Solution> findAllBy(Pageable pageable) {
        return createQuery(pageable, null).all();
    }

    RowsFetchSpec<Solution> createQuery(Pageable pageable, Condition whereClause) {
        List<Expression> columns = SolutionSqlHelper.getColumns(entityTable, EntityManager.ENTITY_ALIAS);
        columns.addAll(OpportunitySqlHelper.getColumns(opportunityTable, "opportunity"));
        columns.addAll(UserSqlHelper.getColumns(ownerTable, "owner"));
        SelectFromAndJoinCondition selectFrom = Select.builder()
            .select(columns)
            .from(entityTable)
            .leftOuterJoin(opportunityTable)
            .on(Column.create("opportunity_id", entityTable))
            .equals(Column.create("id", opportunityTable))
            .leftOuterJoin(ownerTable)
            .on(Column.create("owner_id", entityTable))
            .equals(Column.create("id", ownerTable));
        // we do not support Criteria here for now as of https://github.com/jhipster/generator-jhipster/issues/18269
        String select = entityManager.createSelect(selectFrom, Solution.class, pageable, whereClause);
        return db.sql(select).map(this::process);
    }

    @Override
    public Flux<Solution> findAll() {
        return findAllBy(null);
    }

    @Override
    public Mono<Solution> findById(Long id) {
        Comparison whereClause = Conditions.isEqual(entityTable.column("id"), Conditions.just(id.toString()));
        return createQuery(null, whereClause).one();
    }

    @Override
    public Mono<Solution> findOneWithEagerRelationships(Long id) {
        return findById(id);
    }

    @Override
    public Flux<Solution> findAllWithEagerRelationships() {
        return findAll();
    }

    @Override
    public Flux<Solution> findAllWithEagerRelationships(Pageable page) {
        return findAllBy(page);
    }

    private Solution process(Row row, RowMetadata metadata) {
        Solution entity = solutionMapper.apply(row, "e");
        entity.setOpportunity(opportunityMapper.apply(row, "opportunity"));
        entity.setOwner(userMapper.apply(row, "owner"));
        return entity;
    }

    @Override
    public <S extends Solution> Mono<S> save(S entity) {
        return super.save(entity).flatMap((S e) -> updateRelations(e));
    }

    protected <S extends Solution> Mono<S> updateRelations(S entity) {
        Mono<Void> result = entityManager.updateLinkTable(tagLink, entity.getId(), entity.getTags().stream().map(Tag::getId)).then();
        return result.thenReturn(entity);
    }

    @Override
    public Mono<Void> deleteById(Long entityId) {
        return deleteRelations(entityId).then(super.deleteById(entityId));
    }

    protected Mono<Void> deleteRelations(Long entityId) {
        return entityManager.deleteFromLinkTable(tagLink, entityId);
    }
}
