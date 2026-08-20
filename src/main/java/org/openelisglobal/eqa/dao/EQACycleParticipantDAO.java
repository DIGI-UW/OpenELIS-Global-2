package org.openelisglobal.eqa.dao;

import java.util.Collection;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.eqa.valueholder.EQACycleParticipant;

public interface EQACycleParticipantDAO extends BaseDAO<EQACycleParticipant, Long> {

    /**
     * Active roster rows for a batch of cycles, oldest enrollment first. Batched
     * rather than per-cycle because the provider scheme list needs the roster of
     * every cycle it renders (T-24) — one query, not one per row.
     *
     * <p>
     * Answers an empty list for an empty batch rather than issuing {@code IN ()},
     * which is not valid SQL.
     */
    List<EQACycleParticipant> findActiveByCycleIds(Collection<Long> cycleIds);
}
