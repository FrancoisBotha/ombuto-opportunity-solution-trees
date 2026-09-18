package com.opportunity.tree.repository.rowmapper;

import com.opportunity.tree.domain.Comment;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Comment}, with proper type conversions.
 */
@Service
public class CommentRowMapper implements BiFunction<Row, String, Comment> {

    private final ColumnConverter converter;

    public CommentRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Comment} stored in the database.
     */
    @Override
    public Comment apply(Row row, String prefix) {
        Comment entity = new Comment();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setBody(converter.fromRow(row, prefix + "_body", String.class));
        entity.setCreatedDate(converter.fromRow(row, prefix + "_created_date", Instant.class));
        entity.setEditedDate(converter.fromRow(row, prefix + "_edited_date", Instant.class));
        entity.setAuthorId(converter.fromRow(row, prefix + "_author_id", String.class));
        entity.setParentId(converter.fromRow(row, prefix + "_parent_id", Long.class));
        entity.setOutcomeId(converter.fromRow(row, prefix + "_outcome_id", Long.class));
        entity.setOpportunityId(converter.fromRow(row, prefix + "_opportunity_id", Long.class));
        entity.setSolutionId(converter.fromRow(row, prefix + "_solution_id", Long.class));
        return entity;
    }
}
