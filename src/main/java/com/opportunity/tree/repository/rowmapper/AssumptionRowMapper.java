package com.opportunity.tree.repository.rowmapper;

import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.enumeration.AssumptionCategory;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Assumption}, with proper type conversions.
 */
@Service
public class AssumptionRowMapper implements BiFunction<Row, String, Assumption> {

    private final ColumnConverter converter;

    public AssumptionRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Assumption} stored in the database.
     */
    @Override
    public Assumption apply(Row row, String prefix) {
        Assumption entity = new Assumption();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setStatement(converter.fromRow(row, prefix + "_statement", String.class));
        entity.setCategory(converter.fromRow(row, prefix + "_category", AssumptionCategory.class));
        entity.setImportance(converter.fromRow(row, prefix + "_importance", Integer.class));
        entity.setEvidence(converter.fromRow(row, prefix + "_evidence", Integer.class));
        entity.setValidated(converter.fromRow(row, prefix + "_validated", Boolean.class));
        entity.setCreatedDate(converter.fromRow(row, prefix + "_created_date", Instant.class));
        entity.setSolutionId(converter.fromRow(row, prefix + "_solution_id", Long.class));
        return entity;
    }
}
