package org.openelisglobal.qaevent.criticalcallback.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.qaevent.criticalcallback.dao.CriticalCallbackDAO;
import org.openelisglobal.qaevent.criticalcallback.valueholder.CriticalCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Plain (non-audited) service: a callback row is write-once and is itself the
 * record, so it skips AuditableBaseObjectServiceImpl and the reference_tables
 * registration that would require a numeric id. The compliance compute (OGC-714
 * read side) grows on this service in the follow-up PR.
 */
@Service
public class CriticalCallbackServiceImpl extends BaseObjectServiceImpl<CriticalCallback, String>
        implements CriticalCallbackService {

    @Autowired
    protected CriticalCallbackDAO baseObjectDAO;

    CriticalCallbackServiceImpl() {
        super(CriticalCallback.class);
    }

    @Override
    protected CriticalCallbackDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CriticalCallback> getByAnalysisId(String analysisId) {
        return baseObjectDAO.getByAnalysisId(analysisId);
    }
}
