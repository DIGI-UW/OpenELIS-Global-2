package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerPendingCodeDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerPendingCode;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerPendingCodeServiceTest {

    @Mock
    private AnalyzerPendingCodeDAO analyzerPendingCodeDAO;

    @InjectMocks
    private AnalyzerPendingCodeServiceImpl service;

    @Before
    public void setUp() {
        when(analyzerPendingCodeDAO.update(any(AnalyzerPendingCode.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(analyzerPendingCodeDAO.insert(any(AnalyzerPendingCode.class))).thenReturn("pc-new");
    }

    @Test
    public void testTrack_NewCode_CreatesEntry() {
        when(analyzerPendingCodeDAO.deletePendingOlderThan(eq("101"), any(Timestamp.class))).thenReturn(0);
        when(analyzerPendingCodeDAO.findByAnalyzerAndCode("101", "ABC")).thenReturn(Optional.empty());
        when(analyzerPendingCodeDAO.countByAnalyzerIdAndStatus("101", AnalyzerPendingCode.Status.PENDING)).thenReturn(0L);

        AnalyzerPendingCode created = service.track("101", "ABC", "payload", "1");

        assertNotNull(created);
        assertEquals("101", created.getAnalyzerId());
        assertEquals("ABC", created.getAnalyzerTestName());
        assertEquals(Integer.valueOf(1), created.getSeenCount());
        assertEquals(AnalyzerPendingCode.Status.PENDING, created.getStatus());
        verify(analyzerPendingCodeDAO).insert(any(AnalyzerPendingCode.class));
    }

    @Test
    public void testTrack_ExistingCode_IncrementsCount() {
        AnalyzerPendingCode existing = new AnalyzerPendingCode();
        existing.setId("pc-1");
        existing.setAnalyzerId("101");
        existing.setAnalyzerTestName("ABC");
        existing.setSeenCount(2);
        existing.setStatus(AnalyzerPendingCode.Status.PENDING);

        when(analyzerPendingCodeDAO.deletePendingOlderThan(eq("101"), any(Timestamp.class))).thenReturn(0);
        when(analyzerPendingCodeDAO.findByAnalyzerAndCode("101", "ABC")).thenReturn(Optional.of(existing));

        AnalyzerPendingCode updated = service.track("101", "ABC", "payload-2", "1");

        assertNotNull(updated);
        assertEquals(Integer.valueOf(3), updated.getSeenCount());
        assertEquals("payload-2", updated.getSamplePayload());
        verify(analyzerPendingCodeDAO).update(existing);
    }

    @Test
    public void testTrack_AtCap_EnforcesLimit() {
        when(analyzerPendingCodeDAO.deletePendingOlderThan(eq("101"), any(Timestamp.class))).thenReturn(0);
        when(analyzerPendingCodeDAO.findByAnalyzerAndCode("101", "NEW_CODE")).thenReturn(Optional.empty());
        when(analyzerPendingCodeDAO.countByAnalyzerIdAndStatus("101", AnalyzerPendingCode.Status.PENDING)).thenReturn(100L);

        AnalyzerPendingCode created = service.track("101", "NEW_CODE", "payload", "1");

        assertNull(created);
    }

    @Test
    public void testPurgeExpired_RemovesOldEntries() {
        when(analyzerPendingCodeDAO.deletePendingOlderThan(eq("101"), any(Timestamp.class))).thenReturn(4);

        int deleted = service.purgeExpired("101");

        assertEquals(4, deleted);
    }

    @Test
    public void testUpdateStatus_ValidTransition_Succeeds() {
        AnalyzerPendingCode existing = new AnalyzerPendingCode();
        existing.setId("pc-2");
        existing.setStatus(AnalyzerPendingCode.Status.PENDING);
        when(analyzerPendingCodeDAO.get("pc-2")).thenReturn(Optional.of(existing));

        AnalyzerPendingCode updated = service.updateStatus("pc-2", AnalyzerPendingCode.Status.MAPPED, "1");

        assertEquals(AnalyzerPendingCode.Status.MAPPED, updated.getStatus());
        assertNotNull(updated.getLastSeenAt());
        verify(analyzerPendingCodeDAO).update(existing);
    }
}
