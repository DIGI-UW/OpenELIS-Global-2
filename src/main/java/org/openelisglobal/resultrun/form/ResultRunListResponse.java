package org.openelisglobal.resultrun.form;

import java.util.List;

/** The Runs chip: how many runs are open and held, and which. */
public record ResultRunListResponse(int open, int held, List<ResultRunSummary> runs) {
}
