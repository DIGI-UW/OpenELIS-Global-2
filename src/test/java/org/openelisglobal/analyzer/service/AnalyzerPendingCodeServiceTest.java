package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerPendingCodeDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerPendingCode;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerPendingCodeServiceTest {

    @Mock
    private AnalyzerPendingCodeDAO analyzerPendingCodeDAO;

    @Mock
    private AnalyzerTestMappingService analyzerTestMappingService;

    @Mock
    private org.openelisglobal.test.service.TestService testService;

    @Mock
    private AnalyzerBridgeSyncService analyzerBridgeSyncService;

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
    public void testUpdateStatus_MappedWithoutTestResolution_IsRejected() {
        AnalyzerPendingCode existing = new AnalyzerPendingCode();
        existing.setId("pc-2");
        existing.setAnalyzerId("101");
        existing.setStatus(AnalyzerPendingCode.Status.PENDING);
        when(analyzerPendingCodeDAO.get("pc-2")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateStatus("101", "pc-2", AnalyzerPendingCode.Status.MAPPED, "1"));

        verify(analyzerPendingCodeDAO, never()).update(existing);
        verify(analyzerTestMappingService, never()).insert(any(AnalyzerTestMapping.class));
    }

    @Test
    public void testResolve_CreatesRealTestMappingBeforeMarkingMapped() {
        AnalyzerPendingCode existing = new AnalyzerPendingCode();
        existing.setId("pc-2");
        existing.setAnalyzerId("101");
        existing.setAnalyzerTestName("MTB-RIF");
        existing.setStatus(AnalyzerPendingCode.Status.PENDING);
        when(analyzerPendingCodeDAO.get("pc-2")).thenReturn(Optional.of(existing));

        org.openelisglobal.test.valueholder.Test catalogTest = new org.openelisglobal.test.valueholder.Test();
        catalogTest.setId("501");
        catalogTest.setDescription("Xpert MTB/RIF");
        catalogTest.setLoinc("38379-4");
        when(testService.getAllActiveTests(true)).thenReturn(List.of(catalogTest));
        when(analyzerTestMappingService.getAllForAnalyzer("101")).thenReturn(List.of());

        AnalyzerPendingCode resolved = service.resolve("101", "pc-2", "501", "1");

        assertEquals(AnalyzerPendingCode.Status.MAPPED, resolved.getStatus());
        org.mockito.ArgumentCaptor<AnalyzerTestMapping> mapping = org.mockito.ArgumentCaptor
                .forClass(AnalyzerTestMapping.class);
        verify(analyzerTestMappingService).insert(mapping.capture());
        assertEquals("101", mapping.getValue().getAnalyzerId());
        assertEquals("MTB-RIF", mapping.getValue().getAnalyzerTestName());
        assertEquals("501", mapping.getValue().getTestId());
        verify(analyzerPendingCodeDAO).update(existing);
        verify(analyzerBridgeSyncService).pushAnalyzer("101");
    }

    @Test
    public void testResolve_ToDifferentTest_ClearsStaleComponentBinding() {
        AnalyzerPendingCode existing = new AnalyzerPendingCode();
        existing.setId("pc-2");
        existing.setAnalyzerId("101");
        existing.setAnalyzerTestName("MTB-RIF");
        existing.setStatus(AnalyzerPendingCode.Status.PENDING);
        when(analyzerPendingCodeDAO.get("pc-2")).thenReturn(Optional.of(existing));

        org.openelisglobal.test.valueholder.Test catalogTest = new org.openelisglobal.test.valueholder.Test();
        catalogTest.setId("502");
        catalogTest.setDescription("Replacement test");
        when(testService.getAllActiveTests(true)).thenReturn(List.of(catalogTest));

        AnalyzerTestMapping currentMapping = new AnalyzerTestMapping();
        currentMapping.setAnalyzerId("101");
        currentMapping.setAnalyzerTestName("MTB-RIF");
        currentMapping.setTestId("501");
        currentMapping.setComponentId("component-from-test-501");
        when(analyzerTestMappingService.getAllForAnalyzer("101")).thenReturn(List.of(currentMapping));

        service.resolve("101", "pc-2", "502", "1");

        assertEquals("502", currentMapping.getTestId());
        assertNull(currentMapping.getComponentId());
        verify(analyzerTestMappingService).update(currentMapping);
    }

    @Test
    public void testResolve_InactiveOrIncompleteTest_IsRejected() {
        AnalyzerPendingCode existing = new AnalyzerPendingCode();
        existing.setId("pc-2");
        existing.setAnalyzerId("101");
        existing.setAnalyzerTestName("MTB-RIF");
        existing.setStatus(AnalyzerPendingCode.Status.PENDING);
        when(analyzerPendingCodeDAO.get("pc-2")).thenReturn(Optional.of(existing));
        when(testService.getAllActiveTests(true)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> service.resolve("101", "pc-2", "999", "1"));

        verify(analyzerTestMappingService, never()).insert(any(AnalyzerTestMapping.class));
        verify(analyzerPendingCodeDAO, never()).update(existing);
    }

    @Test
    public void testResolve_MissingCatalogTestId_IsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.resolve("101", "pc-2", " ", "1"));

        verify(analyzerPendingCodeDAO, never()).get(anyString());
        verify(analyzerTestMappingService, never()).insert(any(AnalyzerTestMapping.class));
    }

    @Test
    public void testGetMappingOptions_ReturnsOnlyActiveFullyConfiguredCatalogTests() {
        org.openelisglobal.test.valueholder.Test catalogTest = new org.openelisglobal.test.valueholder.Test();
        catalogTest.setId("501");
        catalogTest.setDescription("Xpert MTB/RIF");
        catalogTest.setLoinc("38379-4");
        when(testService.getAllActiveTests(true)).thenReturn(List.of(catalogTest));

        List<Map<String, Object>> options = service.getMappingOptions();

        assertEquals(1, options.size());
        assertEquals("501", options.get(0).get("id"));
        assertEquals("Xpert MTB/RIF", options.get(0).get("name"));
        assertEquals("38379-4", options.get(0).get("loinc"));
    }

    @Test
    public void testGetMappedTestIds_UsesPersistedAnalyzerTestMappings() {
        AnalyzerTestMapping mapping = new AnalyzerTestMapping();
        mapping.setAnalyzerId("101");
        mapping.setAnalyzerTestName("MTB-RIF");
        mapping.setTestId("501");
        when(analyzerTestMappingService.getAllForAnalyzer("101")).thenReturn(List.of(mapping));

        assertEquals(Map.of("MTB-RIF", "501"), service.getMappedTestIds("101"));
    }

    @Test
    public void testUpdateStatus_ForDifferentAnalyzer_IsRejected() {
        AnalyzerPendingCode existing = new AnalyzerPendingCode();
        existing.setId("pc-2");
        existing.setAnalyzerId("202");
        existing.setStatus(AnalyzerPendingCode.Status.PENDING);
        when(analyzerPendingCodeDAO.get("pc-2")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateStatus("101", "pc-2", AnalyzerPendingCode.Status.MAPPED, "1"));
    }
}
