package com.opportunity.tree.repository.rowmapper;

import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.enumeration.SolutionStatus;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Solution}, with proper type conversions.
 */
@Service
public class SolutionRowMapper implements BiFunction<Row, String, Solution> {

    private final ColumnConverter converter;

    public SolutionRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Solution} stored in the database.
     */
    @Override
    public Solution apply(Row row, String prefix) {
        Solution entity = new Solution();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setTitle(converter.fromRow(row, prefix + "_title", String.class));
        entity.setDescription(converter.fromRow(row, prefix + "_description", String.class));
        entity.setStatus(converter.fromRow(row, prefix + "_status", SolutionStatus.class));
        entity.setEffort(converter.fromRow(row, prefix + "_effort", Integer.class));
        entity.setSortOrder(converter.fromRow(row, prefix + "_sort_order", Integer.class));
        entity.setCreatedDate(converter.fromRow(row, prefix + "_created_date", Instant.class));
        entity.setLastModifiedDate(converter.fromRow(row, prefix + "_last_modified_date", Instant.class));
        entity.setOpportunityId(converter.fromRow(row, prefix + "_opportunity_id", Long.class));
        entity.setOwnerId(converter.fromRow(row, prefix + "_owner_id", String.class));
        return entity;
    }
}
