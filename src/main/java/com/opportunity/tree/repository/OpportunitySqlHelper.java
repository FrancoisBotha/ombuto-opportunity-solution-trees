package com.opportunity.tree.repository;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Table;

public class OpportunitySqlHelper {

    public static List<Expression> getColumns(Table table, String columnPrefix) {
        List<Expression> columns = new ArrayList<>();
        columns.add(Column.aliased("id", table, columnPrefix + "_id"));
        columns.add(Column.aliased("title", table, columnPrefix + "_title"));
        columns.add(Column.aliased("description", table, columnPrefix + "_description"));
        columns.add(Column.aliased("status", table, columnPrefix + "_status"));
        columns.add(Column.aliased("value", table, columnPrefix + "_value"));
        columns.add(Column.aliased("complexity", table, columnPrefix + "_complexity"));
        columns.add(Column.aliased("sort_order", table, columnPrefix + "_sort_order"));
        columns.add(Column.aliased("created_date", table, columnPrefix + "_created_date"));
        columns.add(Column.aliased("last_modified_date", table, columnPrefix + "_last_modified_date"));

        columns.add(Column.aliased("outcome_id", table, columnPrefix + "_outcome_id"));
        columns.add(Column.aliased("parent_id", table, columnPrefix + "_parent_id"));
        columns.add(Column.aliased("owner_id", table, columnPrefix + "_owner_id"));
        return columns;
    }
}
