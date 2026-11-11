package org.openelisglobal.reports.action.implementation.reportBeans;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.openelisglobal.test.valueholder.Test;

/** SQL identifiers and ordered categories shared by CSV export pivots. */
final class CSVColumnSql {
    private CSVColumnSql() {
    }

    static String resultColumnName(Test test) {
        return "test_" + Long.parseLong(test.getId());
    }

    /** Keep category positions identical to the declared result columns. */
    static String categorySql(List<String> ids) {
        String numbers = ids.stream().map(id -> Long.toString(Long.parseLong(id))).collect(Collectors.joining(","));
        return "SELECT id FROM unnest(ARRAY[" + numbers
                + "]::bigint[]) WITH ORDINALITY AS category(id, position) ORDER BY position";
    }

    /** Preserve the lowercase names previously produced by unquoted identifiers. */
    static String observationColumnName(String name) {
        return "\"" + name.toLowerCase(Locale.ROOT).replace("\"", "\"\"") + "\"";
    }
}
