package org.openelisglobal.notebook.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.notebook.dao.NotebookEntryRoomEnvironmentLogDAO;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.notebook.valueholder.NotebookEntryRoomEnvironmentLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for NotebookEntryRoomEnvironmentLog operations.
 */
@Service
public class NotebookEntryRoomEnvironmentLogServiceImpl
        extends AuditableBaseObjectServiceImpl<NotebookEntryRoomEnvironmentLog, Integer>
        implements NotebookEntryRoomEnvironmentLogService {

    @Autowired
    private NotebookEntryRoomEnvironmentLogDAO roomEnvironmentLogDAO;

    @Autowired
    private NotebookAuditService notebookAuditService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    public NotebookEntryRoomEnvironmentLogServiceImpl() {
        super(NotebookEntryRoomEnvironmentLog.class);
    }

    @Override
    protected NotebookEntryRoomEnvironmentLogDAO getBaseObjectDAO() {
        return roomEnvironmentLogDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookEntryRoomEnvironmentLog> findByEntryId(Integer entryId) {
        return roomEnvironmentLogDAO.findByEntryId(entryId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookEntryRoomEnvironmentLog> findByEntryIdAndRoomId(Integer entryId, String roomId) {
        return roomEnvironmentLogDAO.findByEntryIdAndRoomId(entryId, roomId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countByEntryId(Integer entryId) {
        return roomEnvironmentLogDAO.countByEntryId(entryId);
    }

    @Override
    @Transactional
    public NotebookEntryRoomEnvironmentLog logRoomEnvironment(Integer entryId, String roomId, String roomName,
            Double oxygenLevel, Double humidity, String checkedBy, Timestamp checkedDateTime, String notes,
            String sysUserId) {

        NotebookEntry entry = notebookEntryService.get(entryId);
        if (entry == null) {
            throw new IllegalArgumentException("Notebook entry not found with ID: " + entryId +
                ". This may indicate a data consistency issue. Please refresh the page or contact support if the problem persists.");
        }

        NotebookEntryRoomEnvironmentLog log = new NotebookEntryRoomEnvironmentLog();
        log.setNotebookEntry(entry);
        log.setRoomId(roomId);
        log.setRoomName(roomName);
        log.setOxygenLevel(oxygenLevel);
        log.setHumidity(humidity);
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

    @Override
    @Transactional
    public Integer insert(NotebookEntryRoomEnvironmentLog notebookEntryRoomEnvironmentLog) {
        Integer id = super.insert(notebookEntryRoomEnvironmentLog);
        try {
            notebookAuditService.saveAuditLog(notebookEntryRoomEnvironmentLog, "notebook_entry_room_environment_log",
                    "I", notebookEntryRoomEnvironmentLog.getSysUserId());
        } catch (Exception e) {
            LogEvent.logWarn("NotebookEntryRoomEnvironmentLogService", "insert",
                    "Failed to save audit log: " + e.getMessage());
        }
        return id;
    }
}
