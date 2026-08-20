package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.dao.AnalyzerEventDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerEventPersistenceServiceImpl implements AnalyzerEventPersistenceService {

    private final AnalyzerEventDAO eventDAO;

    public AnalyzerEventPersistenceServiceImpl(AnalyzerEventDAO eventDAO) {
        this.eventDAO = eventDAO;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AnalyzerEvent createIfAbsent(AnalyzerEvent event) {
        return eventDAO.getByExternalEventId(event.getExternalEventId()).orElseGet(() -> {
            eventDAO.insert(event);
            return event;
        });
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
}
