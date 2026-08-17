package org.openelisglobal.eqa.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.eqa.valueholder.EQACycleStateTransition;

public interface EQACycleStateTransitionDAO extends BaseDAO<EQACycleStateTransition, Long> {

    /** Oldest first — this is a timeline, not a log tail. */
    List<EQACycleStateTransition> findByCycleId(Long cycleId);
}
