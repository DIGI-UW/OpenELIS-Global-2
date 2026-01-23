package org.openelisglobal.notebook.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.notebook.dao.NotebookEntryTemperatureLogDAO;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.notebook.valueholder.NotebookEntryTemperatureLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for NotebookEntryTemperatureLog operations.
 */
@Service
public class NotebookEntryTemperatureLogServiceImpl
        extends AuditableBaseObjectServiceImpl<NotebookEntryTemperatureLog, Integer>
        implements NotebookEntryTemperatureLogService {

    @Autowired
    private NotebookEntryTemperatureLogDAO temperatureLogDAO;

    @Autowired
    private NotebookEntryService notebookEntryService;

    public NotebookEntryTemperatureLogServiceImpl() {
        super(NotebookEntryTemperatureLog.class);
    }

    @Override
    protected NotebookEntryTemperatureLogDAO getBaseObjectDAO() {
        return temperatureLogDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookEntryTemperatureLog> findByEntryId(Integer entryId) {
        return temperatureLogDAO.findByEntryId(entryId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookEntryTemperatureLog> findByEntryIdAndFreezerId(Integer entryId, String freezerId) {
        return temperatureLogDAO.findByEntryIdAndFreezerId(entryId, freezerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countByEntryId(Integer entryId) {
        return temperatureLogDAO.countByEntryId(entryId);
    }

    @Override
    @Transactional
    public NotebookEntryTemperatureLog logTemperature(Integer entryId, String freezerId, String checkTime,
            Double temperatureValue, String temperatureUnit, String checkedBy, Timestamp checkedDateTime, String notes,
            String sysUserId) {

        NotebookEntry entry = notebookEntryService.get(entryId);
        if (entry == null) {
            throw new IllegalArgumentException("Notebook entry not found: " + entryId);
        }

        NotebookEntryTemperatureLog log = new NotebookEntryTemperatureLog();
        log.setNotebookEntry(entry);
        log.setFreezerId(freezerId);
        log.setCheckTime(checkTime != null ? checkTime : "AM");
        log.setTemperatureValue(temperatureValue);
        log.setTemperatureUnit(temperatureUnit != null ? temperatureUnit : "C");
        log.setCheckedBy(checkedBy);
        log.setCheckedDateTime(checkedDateTime != null ? checkedDateTime : new Timestamp(System.currentTimeMillis()));
        log.setNotes(notes);
        log.setLoggedBy(sysUserId);
        log.setLoggedAt(new Timestamp(System.currentTimeMillis()));
        log.setSysUserId(sysUserId);

        Integer id = insert(log);
        log.setId(id);
        return log;
    }
}
