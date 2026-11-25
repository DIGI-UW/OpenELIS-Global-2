package org.openelisglobal.program.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.program.valueholder.pathology.PathologyStageEvent;

/**
 * Reads and writes the stage events of a pathology case, which the case itself
 * does not hold.
 */
public interface PathologyStageEventDAO extends BaseDAO<PathologyStageEvent, Integer> {

    /** Every stage the case entered, oldest first. */
    List<PathologyStageEvent> getForCase(Integer pathologySampleId);

    /**
     * The entry onto the stage that has not been completed yet. A move always
     * completes the entry it leaves, so there is at most one; taking the latest is
     * only a defence.
     */
    Optional<PathologyStageEvent> getOpenEvent(Integer pathologySampleId, PathologyStatus stage);
}
