package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.dao.AnalyzerEventDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerEventInsertService {

    private final AnalyzerEventDAO eventDAO;

    public AnalyzerEventInsertService(AnalyzerEventDAO eventDAO) {
        this.eventDAO = eventDAO;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AnalyzerEvent insert(AnalyzerEvent event) {
        eventDAO.insert(event);
        return event;
    }
}
