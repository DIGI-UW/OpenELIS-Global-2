package org.openelisglobal.resultrun.form;

import java.util.List;

/**
 * A selection in a run (FR-E11a, FR-E12): the rows picked by id, plus, when
 * {@code all} is set, every row the chip matches less the excluded ones. Rows
 * picked outside that chip are kept by id. Never a list of every row.
 */
public record ResultRunSelection(String chip, boolean all, List<String> ids, List<String> excluded) {
}
