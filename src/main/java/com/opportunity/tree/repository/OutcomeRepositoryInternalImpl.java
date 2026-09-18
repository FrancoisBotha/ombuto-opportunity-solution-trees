package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.repository.rowmapper.OutcomeRowMapper;
import com.opportunity.tree.repository.rowmapper.ProductRowMapper;
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
 * Spring Data R2DBC custom repository implementation for the Outcome entity.
 */
@SuppressWarnings("unused")
class OutcomeRepositoryInternalImpl extends SimpleR2dbcRepository<Outcome, Long> implements OutcomeRepositoryInternal {

    private final DatabaseClient db;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final EntityManager entityManager;

    private final ProductRowMapper productMapper;
    private final UserRowMapper userMapper;
    private final OutcomeRowMapper outcomeMapper;

    private static final Table entityTable = Table.aliased("outcome", EntityManager.ENTITY_ALIAS);
    private static final Table productTable = Table.aliased("product", "product");
    private static final Table ownerTable = Table.aliased("jhi_user", "owner");

    public OutcomeRepositoryInternalImpl(
        R2dbcEntityTemplate template,
        EntityManager entityManager,
        ProductRowMapper productMapper,
        UserRowMapper userMapper,
        OutcomeRowMapper outcomeMapper,
        R2dbcEntityOperations entityOperations,
        R2dbcConverter converter
    ) {
        super(
            new MappingRelationalEntityInformation(converter.getMappingContext().getRequiredPersistentEntity(Outcome.class)),
            entityOperations,
            converter
        );
        this.db = template.getDatabaseClient();
        this.r2dbcEntityTemplate = template;
        this.entityManager = entityManager;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
        this.outcomeMapper = outcomeMapper;
    }

    @Override
    public Flux<Outcome> findAllBy(Pageable pageable) {
        return createQuery(pageable, null).all();
    }

    RowsFetchSpec<Outcome> createQuery(Pageable pageable, Condition whereClause) {
        List<Expression> columns = OutcomeSqlHelper.getColumns(entityTable, EntityManager.ENTITY_ALIAS);
        columns.addAll(ProductSqlHelper.getColumns(productTable, "product"));
        columns.addAll(UserSqlHelper.getColumns(ownerTable, "owner"));
        SelectFromAndJoinCondition selectFrom = Select.builder()
            .select(columns)
            .from(entityTable)
            .leftOuterJoin(productTable)
            .on(Column.create("product_id", entityTable))
            .equals(Column.create("id", productTable))
            .leftOuterJoin(ownerTable)
            .on(Column.create("owner_id", entityTable))
            .equals(Column.create("id", ownerTable));
        // we do not support Criteria here for now as of https://github.com/jhipster/generator-jhipster/issues/18269
        String select = entityManager.createSelect(selectFrom, Outcome.class, pageable, whereClause);
        return db.sql(select).map(this::process);
    }

    @Override
    public Flux<Outcome> findAll() {
        return findAllBy(null);
    }

    @Override
    public Mono<Outcome> findById(Long id) {
        Comparison whereClause = Conditions.isEqual(entityTable.column("id"), Conditions.just(id.toString()));
        return createQuery(null, whereClause).one();
    }

    @Override
    public Mono<Outcome> findOneWithEagerRelationships(Long id) {
        return findById(id);
    }

    @Override
    public Flux<Outcome> findAllWithEagerRelationships() {
        return findAll();
    }

    @Override
    public Flux<Outcome> findAllWithEagerRelationships(Pageable page) {
        return findAllBy(page);
    }

    private Outcome process(Row row, RowMetadata metadata) {
        Outcome entity = outcomeMapper.apply(row, "e");
        entity.setProduct(productMapper.apply(row, "product"));
        entity.setOwner(userMapper.apply(row, "owner"));
        return entity;
    }

    @Override
    public <S extends Outcome> Mono<S> save(S entity) {
        return super.save(entity);
    }
}
