package org.openelisglobal.eqa.dao;

import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.eqa.valueholder.EQAPanel;

public interface EQAPanelDAO extends BaseDAO<EQAPanel, Long> {

    /** Row-locked read, so a state check and its write cannot interleave. */
    Optional<EQAPanel> getForUpdate(Long id);
}
