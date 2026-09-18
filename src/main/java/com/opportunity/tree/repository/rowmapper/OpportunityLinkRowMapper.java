package com.opportunity.tree.repository.rowmapper;

import com.opportunity.tree.domain.OpportunityLink;
import com.opportunity.tree.domain.enumeration.LinkType;
import io.r2dbc.spi.Row;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link OpportunityLink}, with proper type conversions.
 */
@Service
public class OpportunityLinkRowMapper implements BiFunction<Row, String, OpportunityLink> {

    private final ColumnConverter converter;

    public OpportunityLinkRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link OpportunityLink} stored in the database.
     */
    @Override
    public OpportunityLink apply(Row row, String prefix) {
        OpportunityLink entity = new OpportunityLink();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setName(converter.fromRow(row, prefix + "_name", String.class));
        entity.setUrl(converter.fromRow(row, prefix + "_url", String.class));
        entity.setType(converter.fromRow(row, prefix + "_type", LinkType.class));
        entity.setSortOrder(converter.fromRow(row, prefix + "_sort_order", Integer.class));
        entity.setOpportunityId(converter.fromRow(row, prefix + "_opportunity_id", Long.class));
        return entity;
    }
}
