package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Comment;
import com.opportunity.tree.repository.rowmapper.CommentRowMapper;
import com.opportunity.tree.repository.rowmapper.CommentRowMapper;
import com.opportunity.tree.repository.rowmapper.OpportunityRowMapper;
import com.opportunity.tree.repository.rowmapper.OutcomeRowMapper;
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
 * Spring Data R2DBC custom repository implementation for the Comment entity.
 */
@SuppressWarnings("unused")
class CommentRepositoryInternalImpl extends SimpleR2dbcRepository<Comment, Long> implements CommentRepositoryInternal {

    private final DatabaseClient db;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final EntityManager entityManager;

    private final UserRowMapper userMapper;
    private final CommentRowMapper commentMapper;
    private final OutcomeRowMapper outcomeMapper;
    private final OpportunityRowMapper opportunityMapper;
    private final SolutionRowMapper solutionMapper;

    private static final Table entityTable = Table.aliased("comment", EntityManager.ENTITY_ALIAS);
    private static final Table authorTable = Table.aliased("jhi_user", "author");
    private static final Table parentTable = Table.aliased("comment", "parent");
    private static final Table outcomeTable = Table.aliased("outcome", "outcome");
    private static final Table opportunityTable = Table.aliased("opportunity", "opportunity");
    private static final Table solutionTable = Table.aliased("solution", "solution");

    public CommentRepositoryInternalImpl(
        R2dbcEntityTemplate template,
        EntityManager entityManager,
        UserRowMapper userMapper,
        CommentRowMapper commentMapper,
        OutcomeRowMapper outcomeMapper,
        OpportunityRowMapper opportunityMapper,
        SolutionRowMapper solutionMapper,
        R2dbcEntityOperations entityOperations,
        R2dbcConverter converter
    ) {
        super(
            new MappingRelationalEntityInformation(converter.getMappingContext().getRequiredPersistentEntity(Comment.class)),
            entityOperations,
            converter
        );
        this.db = template.getDatabaseClient();
        this.r2dbcEntityTemplate = template;
        this.entityManager = entityManager;
        this.userMapper = userMapper;
        this.commentMapper = commentMapper;
        this.outcomeMapper = outcomeMapper;
        this.opportunityMapper = opportunityMapper;
        this.solutionMapper = solutionMapper;
    }

    @Override
    public Flux<Comment> findAllBy(Pageable pageable) {
        return createQuery(pageable, null).all();
    }

    RowsFetchSpec<Comment> createQuery(Pageable pageable, Condition whereClause) {
        List<Expression> columns = CommentSqlHelper.getColumns(entityTable, EntityManager.ENTITY_ALIAS);
        columns.addAll(UserSqlHelper.getColumns(authorTable, "author"));
        columns.addAll(CommentSqlHelper.getColumns(parentTable, "parent"));
        columns.addAll(OutcomeSqlHelper.getColumns(outcomeTable, "outcome"));
        columns.addAll(OpportunitySqlHelper.getColumns(opportunityTable, "opportunity"));
        columns.addAll(SolutionSqlHelper.getColumns(solutionTable, "solution"));
        SelectFromAndJoinCondition selectFrom = Select.builder()
            .select(columns)
            .from(entityTable)
            .leftOuterJoin(authorTable)
            .on(Column.create("author_id", entityTable))
            .equals(Column.create("id", authorTable))
            .leftOuterJoin(parentTable)
            .on(Column.create("parent_id", entityTable))
            .equals(Column.create("id", parentTable))
            .leftOuterJoin(outcomeTable)
            .on(Column.create("outcome_id", entityTable))
            .equals(Column.create("id", outcomeTable))
            .leftOuterJoin(opportunityTable)
            .on(Column.create("opportunity_id", entityTable))
            .equals(Column.create("id", opportunityTable))
            .leftOuterJoin(solutionTable)
            .on(Column.create("solution_id", entityTable))
            .equals(Column.create("id", solutionTable));
        // we do not support Criteria here for now as of https://github.com/jhipster/generator-jhipster/issues/18269
        String select = entityManager.createSelect(selectFrom, Comment.class, pageable, whereClause);
        return db.sql(select).map(this::process);
    }

    @Override
    public Flux<Comment> findAll() {
        return findAllBy(null);
    }

    @Override
    public Mono<Comment> findById(Long id) {
        Comparison whereClause = Conditions.isEqual(entityTable.column("id"), Conditions.just(id.toString()));
        return createQuery(null, whereClause).one();
    }

    @Override
    public Mono<Comment> findOneWithEagerRelationships(Long id) {
        return findById(id);
    }

    @Override
    public Flux<Comment> findAllWithEagerRelationships() {
        return findAll();
    }

    @Override
    public Flux<Comment> findAllWithEagerRelationships(Pageable page) {
        return findAllBy(page);
    }

    private Comment process(Row row, RowMetadata metadata) {
        Comment entity = commentMapper.apply(row, "e");
        entity.setAuthor(userMapper.apply(row, "author"));
        entity.setParent(commentMapper.apply(row, "parent"));
        entity.setOutcome(outcomeMapper.apply(row, "outcome"));
        entity.setOpportunity(opportunityMapper.apply(row, "opportunity"));
        entity.setSolution(solutionMapper.apply(row, "solution"));
        return entity;
    }

    @Override
    public <S extends Comment> Mono<S> save(S entity) {
        return super.save(entity);
    }
}
