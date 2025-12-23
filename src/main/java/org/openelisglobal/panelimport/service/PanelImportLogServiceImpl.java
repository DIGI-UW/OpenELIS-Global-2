package org.openelisglobal.panelimport.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.panelimport.dao.PanelImportLogDAO;
import org.openelisglobal.panelimport.valueholder.PanelImportLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PanelImportLogServiceImpl extends AuditableBaseObjectServiceImpl<PanelImportLog, String>
        implements PanelImportLogService {
    @Autowired
    protected PanelImportLogDAO baseObjectDAO;

    PanelImportLogServiceImpl() {
        super(PanelImportLog.class);
    }

    @Override
    protected PanelImportLogDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
