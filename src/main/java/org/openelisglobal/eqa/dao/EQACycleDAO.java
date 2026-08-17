package org.openelisglobal.eqa.dao;

import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.eqa.valueholder.EQACycle;

public interface EQACycleDAO extends BaseDAO<EQACycle, Long> {

    /**
     * Row-locked read (SELECT ... FOR UPDATE) so two concurrent transitions cannot
     * both pass the edge check against the same prior state.
     */
    Optional<EQACycle> getForUpdate(Long id);
}
