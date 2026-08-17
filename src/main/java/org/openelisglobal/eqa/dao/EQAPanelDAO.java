package org.openelisglobal.eqa.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.eqa.valueholder.EQAPanel;

public interface EQAPanelDAO extends BaseDAO<EQAPanel, Long> {

    List<EQAPanel> findByCycleId(Long cycleId);
}
