package com.opportunity.tree.repository.rowmapper;

import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Opportunity}, with proper type conversions.
 */
@Service
public class OpportunityRowMapper implements BiFunction<Row, String, Opportunity> {

    private final ColumnConverter converter;

    public OpportunityRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Opportunity} stored in the database.
     */
    @Override
    public Opportunity apply(Row row, String prefix) {
        Opportunity entity = new Opportunity();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setTitle(converter.fromRow(row, prefix + "_title", String.class));
        entity.setDescription(converter.fromRow(row, prefix + "_description", String.class));
        entity.setStatus(converter.fromRow(row, prefix + "_status", OpportunityStatus.class));
        entity.setValue(converter.fromRow(row, prefix + "_value", Integer.class));
        entity.setComplexity(converter.fromRow(row, prefix + "_complexity", Integer.class));
        entity.setSortOrder(converter.fromRow(row, prefix + "_sort_order", Integer.class));
        entity.setCreatedDate(converter.fromRow(row, prefix + "_created_date", Instant.class));
        entity.setLastModifiedDate(converter.fromRow(row, prefix + "_last_modified_date", Instant.class));
        entity.setOutcomeId(converter.fromRow(row, prefix + "_outcome_id", Long.class));
        entity.setParentId(converter.fromRow(row, prefix + "_parent_id", Long.class));
        entity.setOwnerId(converter.fromRow(row, prefix + "_owner_id", String.class));
        return entity;
    }
}
