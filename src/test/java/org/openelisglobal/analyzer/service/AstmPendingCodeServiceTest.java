package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AstmPendingCodeDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AstmPendingCode;

@RunWith(MockitoJUnitRunner.class)
public class AstmPendingCodeServiceTest {

    @Mock
    private AstmPendingCodeDAO pendingCodeDAO;

    @Mock
    private AnalyzerService analyzerService;

    @InjectMocks
    private AstmPendingCodeServiceImpl service;

    private Analyzer analyzer;

    @Before
    public void setUp() {
        analyzer = new Analyzer();
        analyzer.setId("1");
        when(analyzerService.get("1")).thenReturn(analyzer);
        when(pendingCodeDAO.findPendingByAnalyzerId("1")).thenReturn(new ArrayList<>());
        when(pendingCodeDAO.countPendingByAnalyzerId("1")).thenReturn(0);
    }

    @Test
    public void recordSeen_NewCode_InsertsPendingCode() {
        service.recordSeen("1", "TBIL", "payload");

        ArgumentCaptor<AstmPendingCode> captor = ArgumentCaptor.forClass(AstmPendingCode.class);
        verify(pendingCodeDAO).insert(captor.capture());
        assertEquals("TBIL", captor.getValue().getAnalyzerTestName());
        assertEquals(AstmPendingCode.Status.PENDING.name(), captor.getValue().getStatus());
        assertEquals(Integer.valueOf(1), captor.getValue().getSeenCount());
    }

    @Test
    public void recordSeen_ExistingCode_IncrementsSeenCount() {
        AstmPendingCode existing = new AstmPendingCode();
        existing.setId("pc-1");
        existing.setAnalyzer(analyzer);
        existing.setAnalyzerTestName("TBIL");
        existing.setSeenCount(2);
        existing.setStatus(AstmPendingCode.Status.PENDING.name());
        when(pendingCodeDAO.findPendingByAnalyzerId("1")).thenReturn(List.of(existing));

        service.recordSeen("1", "TBIL", "payload-2");

        verify(pendingCodeDAO).update(existing);
        assertEquals(Integer.valueOf(3), existing.getSeenCount());
    }

    @Test
    public void recordSeen_WhenQueueAtCap_DoesNotInsertNewCode() {
        when(pendingCodeDAO.countPendingByAnalyzerId("1")).thenReturn(100);

        service.recordSeen("1", "NEWCODE", "payload");

        verify(pendingCodeDAO, never()).insert(any(AstmPendingCode.class));
    }

    @Test
    public void purgeOlderThanDays_DeletesExpiredEntries() {
        AstmPendingCode oldCode = new AstmPendingCode();
        oldCode.setId("old-1");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -31);
        oldCode.setLastSeenAt(cal.getTime());

        AstmPendingCode recentCode = new AstmPendingCode();
        recentCode.setId("new-1");
        recentCode.setLastSeenAt(new Date());

        when(pendingCodeDAO.getAll()).thenReturn(List.of(oldCode, recentCode));

        service.purgeOlderThanDays(30);

        verify(pendingCodeDAO).delete(oldCode);
        verify(pendingCodeDAO, never()).delete(recentCode);
    }

    @Test
    public void resolveByMapping_UpdatesStatusResolved() {
        AstmPendingCode pendingCode = new AstmPendingCode();
        pendingCode.setId("pc-1");
        pendingCode.setStatus(AstmPendingCode.Status.PENDING.name());
        when(pendingCodeDAO.get("pc-1")).thenReturn(Optional.of(pendingCode));

        service.resolveByMapping("pc-1", "test-1");

        verify(pendingCodeDAO).update(pendingCode);
        assertEquals(AstmPendingCode.Status.RESOLVED.name(), pendingCode.getStatus());
    }

    @Test
    public void updateStatus_IgnoreDismiss_UpdatesStatus() {
        AstmPendingCode pendingCode = new AstmPendingCode();
        pendingCode.setId("pc-1");
        pendingCode.setStatus(AstmPendingCode.Status.PENDING.name());
        when(pendingCodeDAO.get("pc-1")).thenReturn(Optional.of(pendingCode));

        service.updateStatus("pc-1", AstmPendingCode.Status.IGNORED);

        verify(pendingCodeDAO).update(pendingCode);
        assertEquals(AstmPendingCode.Status.IGNORED.name(), pendingCode.getStatus());
    }
}
