package org.openelisglobal.history.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.audittrail.dao.HistoryDAO;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoryServiceImpl extends AuditableBaseObjectServiceImpl<History, String> implements HistoryService {
    @Autowired
    protected HistoryDAO baseObjectDAO;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    private Map<String, String> systemAuditReferenceTableIds;

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
    public synchronized Map<String, String> getSystemAuditReferenceTableIds() {
        if (systemAuditReferenceTableIds == null) {
            Map<String, String> nameToId = new LinkedHashMap<>();
            for (String tableName : SYSTEM_AUDIT_ENTITY_TABLES) {
                ReferenceTables referenceTable = referenceTablesService.getReferenceTableByName(tableName);
                if (referenceTable != null) {
                    nameToId.put(tableName, referenceTable.getId());
                }
            }
            systemAuditReferenceTableIds = Collections.unmodifiableMap(nameToId);
        }
        return systemAuditReferenceTableIds;
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
        // These overrides write straight to the DAO (bypassing super), so stamp the
        // audit user here — the base class would otherwise do it in super.insert.
        fillSysUserIdIfMissing(history);
        return baseObjectDAO.insert(history);
    }

    @Override
    public History update(History history) {
        fillSysUserIdIfMissing(history);
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
        fillSysUserIdIfMissing(history);
        baseObjectDAO.delete(history);
    }

    @Override
    @Transactional(readOnly = true)
    public List<History> getSystemEventHistory(Timestamp startDate, Timestamp endDate, String sysUserId,
            List<String> referenceTableIds, String activity, String search, String referenceId, int page, int pageSize)
            throws LIMSRuntimeException {
        return baseObjectDAO.getSystemEventHistory(startDate, endDate, sysUserId, referenceTableIds, activity, search,
                referenceId, page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public long getSystemEventHistoryCount(Timestamp startDate, Timestamp endDate, String sysUserId,
            List<String> referenceTableIds, String activity, String search, String referenceId)
            throws LIMSRuntimeException {
        return baseObjectDAO.getSystemEventHistoryCount(startDate, endDate, sysUserId, referenceTableIds, activity,
                search, referenceId);
    }
}
