package org.openelisglobal.program.dao;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;

public interface PathologySampleDAO extends BaseDAO<PathologySample, Integer> {

    List<PathologySample> getWithStatus(List<PathologyStatus> statuses);

    Long getCountWithStatusBetweenDates(List<PathologyStatus> statuses, Timestamp from, Timestamp to);

    List<PathologySample> searchWithStatusAndAccesionNumber(List<PathologyStatus> statuses, String labNumber);

    Long getCountWithStatus(List<PathologyStatus> statuses);

    /**
     * Counts the cases behind the dashboard's "additional requests" tile: those
     * holding at least one pathology_request still at OPENED (AC-6).
     *
     * <p>
     * An outstanding request used to be recorded by parking the case in the
     * ADDITIONAL_REQUEST stage, which cost the case its real bench stage. It is now
     * read from the request rows themselves, so a case is counted once however many
     * of its requests are open.
     */
    Long getCountWithOpenRequests();

    /** The case a block belongs to, null when there is none. */
    PathologySample getCaseOfBlock(Integer blockId);

    /** The case a slide belongs to, null when there is none. */
    PathologySample getCaseOfSlide(Integer slideId);

    /**
     * Raises the case's version now, locking its row for the rest of the
     * transaction, though nothing on the case row itself changed, so a change made
     * to its slides and a change made to the case in the same moment cannot both
     * commit: the second waits for the first and then loses its version check.
     *
     * <p>
     * A caller that writes slide rows takes the case row first. A save of the case
     * writes the case row before its slides, so taking them in the same order means
     * the two wait for each other instead of deadlocking.
     */
    void forceVersionIncrement(PathologySample pathologySample);
}
