package com.opportunity.tree.repository;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Table;

public class ExperimentSqlHelper {

    public static List<Expression> getColumns(Table table, String columnPrefix) {
        List<Expression> columns = new ArrayList<>();
        columns.add(Column.aliased("id", table, columnPrefix + "_id"));
        columns.add(Column.aliased("title", table, columnPrefix + "_title"));
        columns.add(Column.aliased("hypothesis", table, columnPrefix + "_hypothesis"));
        columns.add(Column.aliased("method", table, columnPrefix + "_method"));
        columns.add(Column.aliased("success_criteria", table, columnPrefix + "_success_criteria"));
        columns.add(Column.aliased("status", table, columnPrefix + "_status"));
        columns.add(Column.aliased("result", table, columnPrefix + "_result"));
        columns.add(Column.aliased("learnings", table, columnPrefix + "_learnings"));
        columns.add(Column.aliased("start_date", table, columnPrefix + "_start_date"));
        columns.add(Column.aliased("end_date", table, columnPrefix + "_end_date"));
        columns.add(Column.aliased("created_date", table, columnPrefix + "_created_date"));

        columns.add(Column.aliased("solution_id", table, columnPrefix + "_solution_id"));
        return columns;
    }
}
