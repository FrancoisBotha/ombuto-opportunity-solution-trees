package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Interview;
import com.opportunity.tree.repository.rowmapper.InterviewRowMapper;
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
 * Spring Data R2DBC custom repository implementation for the Interview entity.
 */
@SuppressWarnings("unused")
class InterviewRepositoryInternalImpl extends SimpleR2dbcRepository<Interview, Long> implements InterviewRepositoryInternal {

    private final DatabaseClient db;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final EntityManager entityManager;

    private final ProductRowMapper productMapper;
    private final UserRowMapper userMapper;
    private final InterviewRowMapper interviewMapper;

    private static final Table entityTable = Table.aliased("interview", EntityManager.ENTITY_ALIAS);
    private static final Table productTable = Table.aliased("product", "product");
    private static final Table interviewerTable = Table.aliased("jhi_user", "interviewer");

    public InterviewRepositoryInternalImpl(
        R2dbcEntityTemplate template,
        EntityManager entityManager,
        ProductRowMapper productMapper,
        UserRowMapper userMapper,
        InterviewRowMapper interviewMapper,
        R2dbcEntityOperations entityOperations,
        R2dbcConverter converter
    ) {
        super(
            new MappingRelationalEntityInformation(converter.getMappingContext().getRequiredPersistentEntity(Interview.class)),
            entityOperations,
            converter
        );
        this.db = template.getDatabaseClient();
        this.r2dbcEntityTemplate = template;
        this.entityManager = entityManager;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
        this.interviewMapper = interviewMapper;
    }

    @Override
    public Flux<Interview> findAllBy(Pageable pageable) {
        return createQuery(pageable, null).all();
    }

    RowsFetchSpec<Interview> createQuery(Pageable pageable, Condition whereClause) {
        List<Expression> columns = InterviewSqlHelper.getColumns(entityTable, EntityManager.ENTITY_ALIAS);
        columns.addAll(ProductSqlHelper.getColumns(productTable, "product"));
        columns.addAll(UserSqlHelper.getColumns(interviewerTable, "interviewer"));
        SelectFromAndJoinCondition selectFrom = Select.builder()
            .select(columns)
            .from(entityTable)
            .leftOuterJoin(productTable)
            .on(Column.create("product_id", entityTable))
            .equals(Column.create("id", productTable))
            .leftOuterJoin(interviewerTable)
            .on(Column.create("interviewer_id", entityTable))
            .equals(Column.create("id", interviewerTable));
        // we do not support Criteria here for now as of https://github.com/jhipster/generator-jhipster/issues/18269
        String select = entityManager.createSelect(selectFrom, Interview.class, pageable, whereClause);
        return db.sql(select).map(this::process);
    }

    @Override
    public Flux<Interview> findAll() {
        return findAllBy(null);
    }

    @Override
    public Mono<Interview> findById(Long id) {
        Comparison whereClause = Conditions.isEqual(entityTable.column("id"), Conditions.just(id.toString()));
        return createQuery(null, whereClause).one();
    }

    @Override
    public Mono<Interview> findOneWithEagerRelationships(Long id) {
        return findById(id);
    }

    @Override
    public Flux<Interview> findAllWithEagerRelationships() {
        return findAll();
    }

    @Override
    public Flux<Interview> findAllWithEagerRelationships(Pageable page) {
        return findAllBy(page);
    }

    private Interview process(Row row, RowMetadata metadata) {
        Interview entity = interviewMapper.apply(row, "e");
        entity.setProduct(productMapper.apply(row, "product"));
        entity.setInterviewer(userMapper.apply(row, "interviewer"));
        return entity;
    }

    @Override
    public <S extends Interview> Mono<S> save(S entity) {
        return super.save(entity);
    }
}
