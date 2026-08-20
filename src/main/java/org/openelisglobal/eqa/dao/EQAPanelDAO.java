package org.openelisglobal.eqa.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.eqa.valueholder.EQAPanel;

public interface EQAPanelDAO extends BaseDAO<EQAPanel, Long> {

    /** Row-locked read, so a state check and its write cannot interleave. */
    Optional<EQAPanel> getForUpdate(Long id);

    /**
     * Panel count per cycle for a batch of cycles, as {@code [cycleId, count]} rows
     * — the provider scheme list's panel column without a query per cycle (T-24).
     * Cycles with no panel are simply absent.
     */
    List<Object[]> countByCycleIds(Collection<Long> cycleIds);
}
