package org.openelisglobal.eqa.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.eqa.valueholder.EQACycle;

public interface EQACycleDAO extends BaseDAO<EQACycle, Long> {

    /**
     * Row-locked read (SELECT ... FOR UPDATE) so two concurrent transitions cannot
     * both pass the edge check against the same prior state.
     */
    Optional<EQACycle> getForUpdate(Long id);

    /**
     * Every cycle of a batch of schemes, newest cycle number first within each
     * scheme — the provider scheme list's cycles in one query (T-24).
     */
    List<EQACycle> findBySchemeIds(Collection<Long> schemeIds);
}
