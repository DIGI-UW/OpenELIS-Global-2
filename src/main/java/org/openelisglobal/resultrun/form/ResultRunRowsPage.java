package org.openelisglobal.resultrun.form;

import java.util.List;

/** One page of a run's rows; {@code totalRows} counts the whole run. */
public record ResultRunRowsPage(List<ResultRunRow> rows, int page, int pageSize, int totalRows) {
}
