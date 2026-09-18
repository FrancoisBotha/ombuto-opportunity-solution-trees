package com.opportunity.tree.repository.rowmapper;

import com.opportunity.tree.domain.Interview;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.time.LocalDate;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Interview}, with proper type conversions.
 */
@Service
public class InterviewRowMapper implements BiFunction<Row, String, Interview> {

    private final ColumnConverter converter;

    public InterviewRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Interview} stored in the database.
     */
    @Override
    public Interview apply(Row row, String prefix) {
        Interview entity = new Interview();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setTitle(converter.fromRow(row, prefix + "_title", String.class));
        entity.setParticipant(converter.fromRow(row, prefix + "_participant", String.class));
        entity.setInterviewDate(converter.fromRow(row, prefix + "_interview_date", LocalDate.class));
        entity.setNotes(converter.fromRow(row, prefix + "_notes", String.class));
        entity.setRecordingUrl(converter.fromRow(row, prefix + "_recording_url", String.class));
        entity.setCreatedDate(converter.fromRow(row, prefix + "_created_date", Instant.class));
        entity.setProductId(converter.fromRow(row, prefix + "_product_id", Long.class));
        entity.setInterviewerId(converter.fromRow(row, prefix + "_interviewer_id", String.class));
        return entity;
    }
}
