package org.openelisglobal.qaevent.service;

import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.qaevent.dao.NceActionLogDAO;
import org.openelisglobal.qaevent.valueholder.NceActionLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NceActionLogServiceImpl extends AuditableBaseObjectServiceImpl<NceActionLog, String>
        implements NceActionLogService {

    @Autowired
    protected NceActionLogDAO baseObjectDAO;

    public NceActionLogServiceImpl() {
        super(NceActionLog.class);
    }

    @Override
    @Transactional
    public List<NceActionLog> getNceActionLogByNceId(String nceId) throws LIMSRuntimeException {
        return getBaseObjectDAO().getNceActionLogByNceId(nceId);
    }

    @Override
    protected NceActionLogDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
