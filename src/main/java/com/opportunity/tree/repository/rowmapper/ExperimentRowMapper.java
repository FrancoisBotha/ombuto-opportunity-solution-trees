package com.opportunity.tree.repository.rowmapper;

import com.opportunity.tree.domain.Experiment;
import com.opportunity.tree.domain.enumeration.ExperimentResult;
import com.opportunity.tree.domain.enumeration.ExperimentStatus;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.time.LocalDate;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Experiment}, with proper type conversions.
 */
@Service
public class ExperimentRowMapper implements BiFunction<Row, String, Experiment> {

    private final ColumnConverter converter;

    public ExperimentRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Experiment} stored in the database.
     */
    @Override
    public Experiment apply(Row row, String prefix) {
        Experiment entity = new Experiment();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setTitle(converter.fromRow(row, prefix + "_title", String.class));
        entity.setHypothesis(converter.fromRow(row, prefix + "_hypothesis", String.class));
        entity.setMethod(converter.fromRow(row, prefix + "_method", String.class));
        entity.setSuccessCriteria(converter.fromRow(row, prefix + "_success_criteria", String.class));
        entity.setStatus(converter.fromRow(row, prefix + "_status", ExperimentStatus.class));
        entity.setResult(converter.fromRow(row, prefix + "_result", ExperimentResult.class));
        entity.setLearnings(converter.fromRow(row, prefix + "_learnings", String.class));
        entity.setStartDate(converter.fromRow(row, prefix + "_start_date", LocalDate.class));
        entity.setEndDate(converter.fromRow(row, prefix + "_end_date", LocalDate.class));
        entity.setCreatedDate(converter.fromRow(row, prefix + "_created_date", Instant.class));
        entity.setSolutionId(converter.fromRow(row, prefix + "_solution_id", Long.class));
        return entity;
    }
}
