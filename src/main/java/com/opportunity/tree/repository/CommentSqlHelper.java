package com.opportunity.tree.repository;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Table;

public class CommentSqlHelper {

    public static List<Expression> getColumns(Table table, String columnPrefix) {
        List<Expression> columns = new ArrayList<>();
        columns.add(Column.aliased("id", table, columnPrefix + "_id"));
        columns.add(Column.aliased("body", table, columnPrefix + "_body"));
        columns.add(Column.aliased("created_date", table, columnPrefix + "_created_date"));
        columns.add(Column.aliased("edited_date", table, columnPrefix + "_edited_date"));

        columns.add(Column.aliased("author_id", table, columnPrefix + "_author_id"));
        columns.add(Column.aliased("parent_id", table, columnPrefix + "_parent_id"));
        columns.add(Column.aliased("outcome_id", table, columnPrefix + "_outcome_id"));
        columns.add(Column.aliased("opportunity_id", table, columnPrefix + "_opportunity_id"));
        columns.add(Column.aliased("solution_id", table, columnPrefix + "_solution_id"));
        return columns;
    }
}
