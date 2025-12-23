package org.openelisglobal.panelimport.dao;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.panelimport.valueholder.PanelImportLog;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class PanelImportLogDAOImpl extends BaseDAOImpl<PanelImportLog, String> implements PanelImportLogDAO {
    PanelImportLogDAOImpl() {
        super(PanelImportLog.class);
    }
}
