package org.openelisglobal.program.valueholder;

import java.util.List;

/**
 * A patient's earlier anatomic-pathology cases as one case view lists them: the
 * rows shown, how many prior cases the viewer may read in all, and whether the
 * viewer reads only some lab units, so cases outside them are not listed.
 */
public record PriorResults(List<PriorResultRow> rows, int total, boolean restricted) {

    public static PriorResults none(boolean restricted) {
        return new PriorResults(List.of(), 0, restricted);
    }
}
