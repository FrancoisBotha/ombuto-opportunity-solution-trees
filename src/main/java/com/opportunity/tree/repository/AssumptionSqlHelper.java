package com.opportunity.tree.repository;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Table;

public class AssumptionSqlHelper {

    public static List<Expression> getColumns(Table table, String columnPrefix) {
        List<Expression> columns = new ArrayList<>();
        columns.add(Column.aliased("id", table, columnPrefix + "_id"));
        columns.add(Column.aliased("statement", table, columnPrefix + "_statement"));
        columns.add(Column.aliased("category", table, columnPrefix + "_category"));
        columns.add(Column.aliased("importance", table, columnPrefix + "_importance"));
        columns.add(Column.aliased("evidence", table, columnPrefix + "_evidence"));
        columns.add(Column.aliased("validated", table, columnPrefix + "_validated"));
        columns.add(Column.aliased("created_date", table, columnPrefix + "_created_date"));

        columns.add(Column.aliased("solution_id", table, columnPrefix + "_solution_id"));
        return columns;
    }
}
