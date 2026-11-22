package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Locale;
import org.hibernate.exception.ConstraintViolationException;
import org.openelisglobal.analyzer.dao.AnalyzerEventDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerEventPersistenceServiceImpl implements AnalyzerEventPersistenceService {

    private static final String EXTERNAL_EVENT_CONSTRAINT = "uq_analyzer_event_external";

    private final AnalyzerEventDAO eventDAO;
    private final AnalyzerEventInsertService insertService;

    public AnalyzerEventPersistenceServiceImpl(AnalyzerEventDAO eventDAO, AnalyzerEventInsertService insertService) {
        this.eventDAO = eventDAO;
        this.insertService = insertService;
    }

    @Override
    public AnalyzerEventRegistration createIfAbsent(AnalyzerEvent event) {
        var existing = eventDAO.getByExternalEventId(event.getExternalEventId());
        if (existing.isPresent()) {
            return new AnalyzerEventRegistration(existing.get(), false);
        }
        try {
            return new AnalyzerEventRegistration(insertService.insert(event), true);
        } catch (RuntimeException exception) {
            if (!isExternalEventConflict(exception)) {
                throw exception;
            }
            AnalyzerEvent winner = eventDAO.getByExternalEventId(event.getExternalEventId())
                    .orElseThrow(() -> exception);
            return new AnalyzerEventRegistration(winner, false);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AnalyzerEvent markApplied(AnalyzerEvent event, String targetReference) {
        event.setStatus("APPLIED");
        event.setTargetReference(targetReference);
        event.setFailureReason(null);
        event.setProcessedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        return eventDAO.update(event);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AnalyzerEvent markFailed(AnalyzerEvent event, String failureReason) {
        event.setStatus("FAILED");
        event.setFailureReason(failureReason);
        event.setProcessedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        return eventDAO.update(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerEvent> getFailed(int limit) {
        return eventDAO.getFailed(limit);
    }

    private boolean isExternalEventConflict(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation) {
                String constraintName = constraintViolation.getConstraintName();
                if (constraintName != null && EXTERNAL_EVENT_CONSTRAINT.equalsIgnoreCase(constraintName)) {
                    return true;
                }
            }
            if (cause instanceof java.sql.SQLException sqlException && "23505".equals(sqlException.getSQLState())
                    && text(sqlException.getMessage()).contains(EXTERNAL_EVENT_CONSTRAINT)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private String text(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
