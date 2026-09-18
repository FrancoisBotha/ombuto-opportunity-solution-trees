package com.opportunity.tree.repository.rowmapper;

import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.enumeration.TeamRole;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link TeamMember}, with proper type conversions.
 */
@Service
public class TeamMemberRowMapper implements BiFunction<Row, String, TeamMember> {

    private final ColumnConverter converter;

    public TeamMemberRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link TeamMember} stored in the database.
     */
    @Override
    public TeamMember apply(Row row, String prefix) {
        TeamMember entity = new TeamMember();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setRole(converter.fromRow(row, prefix + "_role", TeamRole.class));
        entity.setJoinedDate(converter.fromRow(row, prefix + "_joined_date", Instant.class));
        entity.setTeamId(converter.fromRow(row, prefix + "_team_id", Long.class));
        entity.setUserId(converter.fromRow(row, prefix + "_user_id", String.class));
        return entity;
    }
}
