package org.openelisglobal.analysisqaeventaction.service;

import org.openelisglobal.analysisqaeventaction.dao.AnalysisQaEventActionDAO;
import org.openelisglobal.analysisqaeventaction.valueholder.AnalysisQaEventAction;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnalysisQaEventActionServiceImpl extends AuditableBaseObjectServiceImpl<AnalysisQaEventAction, String>
        implements AnalysisQaEventActionService {
    @Autowired
    protected AnalysisQaEventActionDAO baseObjectDAO;

    AnalysisQaEventActionServiceImpl() {
        super(AnalysisQaEventAction.class);
    }

    @Override
    protected AnalysisQaEventActionDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
