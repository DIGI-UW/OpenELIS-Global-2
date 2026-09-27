package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Test;
import org.openelisglobal.analyzer.dao.AnalyzerEventDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerEvent;
import org.openelisglobal.common.exception.LIMSRuntimeException;

public class AnalyzerEventPersistenceServiceTest {

    @Test
    public void reloadsTheWinningEventAfterAConcurrentInsertConflict() {
        AnalyzerEventDAO eventDAO = mock(AnalyzerEventDAO.class);
        AnalyzerEventInsertService insertService = mock(AnalyzerEventInsertService.class);
        AnalyzerEvent candidate = event("event-42");
        AnalyzerEvent winner = event("event-42");
        when(eventDAO.getByExternalEventId("event-42")).thenReturn(Optional.empty(), Optional.of(winner));
        doThrow(externalEventConflict()).when(insertService).insert(candidate);
        AnalyzerEventPersistenceService service = new AnalyzerEventPersistenceServiceImpl(eventDAO, insertService);

        AnalyzerEventRegistration registration = service.createIfAbsent(candidate);

        assertSame(winner, registration.event());
        assertFalse(registration.created());
    }

    @Test
    public void doesNotHideAnUnrelatedInsertFailure() {
        AnalyzerEventDAO eventDAO = mock(AnalyzerEventDAO.class);
        AnalyzerEventInsertService insertService = mock(AnalyzerEventInsertService.class);
        AnalyzerEvent candidate = event("event-43");
        LIMSRuntimeException failure = new LIMSRuntimeException("database unavailable");
        when(eventDAO.getByExternalEventId("event-43")).thenReturn(Optional.empty());
        doThrow(failure).when(insertService).insert(candidate);
        AnalyzerEventPersistenceService service = new AnalyzerEventPersistenceServiceImpl(eventDAO, insertService);

        LIMSRuntimeException thrown = assertThrows(LIMSRuntimeException.class, () -> service.createIfAbsent(candidate));

        assertSame(failure, thrown);
    }

    private LIMSRuntimeException externalEventConflict() {
        SQLException sqlException = new SQLException("duplicate key violates uq_analyzer_event_external", "23505");
        ConstraintViolationException conflict = new ConstraintViolationException("duplicate analyzer event",
                sqlException, "insert", "uq_analyzer_event_external");
        return new LIMSRuntimeException("insert failed", conflict);
    }

    private AnalyzerEvent event(String externalEventId) {
        AnalyzerEvent event = new AnalyzerEvent();
        event.setExternalEventId(externalEventId);
        event.setEventType("AST_RESULT_AVAILABLE");
        event.setPayload("{}");
        return event;
    }
}
