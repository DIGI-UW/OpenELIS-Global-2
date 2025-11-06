package org.openelisglobal.history.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.openelisglobal.audittrail.dao.HistoryDAO;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoryServiceImpl extends AuditableBaseObjectServiceImpl<History, String> implements HistoryService {
    @Autowired
    protected HistoryDAO baseObjectDAO;

    HistoryServiceImpl() {
        super(History.class);
        disableLogging();
    }

    @Override
    protected HistoryDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<History> getHistoryByRefIdAndRefTableId(History history) throws LIMSRuntimeException {
        return baseObjectDAO.getHistoryByRefIdAndRefTableId(history);
    }

    @Override
    @Transactional(readOnly = true)
    public List<History> getHistoryByRefIdAndRefTableId(String id, String table) throws LIMSRuntimeException {
        return baseObjectDAO.getHistoryByRefIdAndRefTableId(id, table);
    }

    @Override
    public String insert(History history) {
        return baseObjectDAO.insert(history);
    }

    @Override
    public History update(History history) {
        if (history.getLastupdated() == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "update",
                    "running update on an object with a missing version field can result in unintended"
                            + " inserts instead of updates");
            LogEvent.logWarn(this.getClass().getSimpleName(), "update", "setting lastUpdated to now for object: "
                    + history.getClass().getSimpleName() + " with id: " + history.getId());
            history.setLastupdated(Timestamp.from(Instant.now()));
        }
        return baseObjectDAO.update(history);
    }

    @Override
    public void delete(History history) {
        baseObjectDAO.delete(history);
    }
}
