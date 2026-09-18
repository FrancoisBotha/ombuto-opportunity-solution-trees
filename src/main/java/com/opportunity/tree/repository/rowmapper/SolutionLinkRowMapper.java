package com.opportunity.tree.repository.rowmapper;

import com.opportunity.tree.domain.SolutionLink;
import com.opportunity.tree.domain.enumeration.LinkType;
import io.r2dbc.spi.Row;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link SolutionLink}, with proper type conversions.
 */
@Service
public class SolutionLinkRowMapper implements BiFunction<Row, String, SolutionLink> {

    private final ColumnConverter converter;

    public SolutionLinkRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link SolutionLink} stored in the database.
     */
    @Override
    public SolutionLink apply(Row row, String prefix) {
        SolutionLink entity = new SolutionLink();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setName(converter.fromRow(row, prefix + "_name", String.class));
        entity.setUrl(converter.fromRow(row, prefix + "_url", String.class));
        entity.setType(converter.fromRow(row, prefix + "_type", LinkType.class));
        entity.setSortOrder(converter.fromRow(row, prefix + "_sort_order", Integer.class));
        entity.setSolutionId(converter.fromRow(row, prefix + "_solution_id", Long.class));
        return entity;
    }
}
