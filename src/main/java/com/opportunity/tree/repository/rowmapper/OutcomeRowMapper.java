package com.opportunity.tree.repository.rowmapper;

import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.enumeration.OutcomeStatus;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.time.LocalDate;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Outcome}, with proper type conversions.
 */
@Service
public class OutcomeRowMapper implements BiFunction<Row, String, Outcome> {

    private final ColumnConverter converter;

    public OutcomeRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Outcome} stored in the database.
     */
    @Override
    public Outcome apply(Row row, String prefix) {
        Outcome entity = new Outcome();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setTitle(converter.fromRow(row, prefix + "_title", String.class));
        entity.setDescription(converter.fromRow(row, prefix + "_description", String.class));
        entity.setMetric(converter.fromRow(row, prefix + "_metric", String.class));
        entity.setTargetValue(converter.fromRow(row, prefix + "_target_value", String.class));
        entity.setCurrentValue(converter.fromRow(row, prefix + "_current_value", String.class));
        entity.setStatus(converter.fromRow(row, prefix + "_status", OutcomeStatus.class));
        entity.setStartDate(converter.fromRow(row, prefix + "_start_date", LocalDate.class));
        entity.setTargetDate(converter.fromRow(row, prefix + "_target_date", LocalDate.class));
        entity.setSortOrder(converter.fromRow(row, prefix + "_sort_order", Integer.class));
        entity.setCreatedDate(converter.fromRow(row, prefix + "_created_date", Instant.class));
        entity.setLastModifiedDate(converter.fromRow(row, prefix + "_last_modified_date", Instant.class));
        entity.setProductId(converter.fromRow(row, prefix + "_product_id", Long.class));
        entity.setOwnerId(converter.fromRow(row, prefix + "_owner_id", String.class));
        return entity;
    }
}
