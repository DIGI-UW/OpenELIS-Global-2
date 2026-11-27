package org.openelisglobal.program.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.program.valueholder.pathology.PathologyReport;
import org.openelisglobal.program.valueholder.pathology.PathologySample;

/**
 * Reads and writes the report versions of a pathology case, which the case
 * itself does not hold.
 */
public interface PathologyReportDAO extends BaseDAO<PathologyReport, Integer> {

    /**
     * Every row of the case with its file bytes, newest version first, rows without
     * a number last. Only a check that compares stored files reads this.
     */
    List<PathologyReport> getForCase(Integer pathologySampleId);

    /**
     * The case's rows that hold a file, in the order of {@link #getForCase}, read
     * without the file bytes: each is a copy the session does not hold, its image
     * empty, so it is read and never saved. A row to change is loaded with
     * {@link #getOnCase}.
     */
    List<PathologyReport> getListedForCase(Integer pathologySampleId);

    /**
     * The row with its file bytes, when it is one of this case's; empty for any
     * other id.
     */
    Optional<PathologyReport> getOnCase(Integer pathologySampleId, Integer reportId);

    /** The case's highest version number plus one, 1 when it has none. */
    int nextVersion(Integer pathologySampleId);

    /**
     * Takes a write lock on the case's own row until the transaction ends, without
     * raising its version, so report writes on one case run one after another.
     *
     * @return the case, or null when no case carries the id
     */
    PathologySample lockCase(Integer pathologySampleId);
}
