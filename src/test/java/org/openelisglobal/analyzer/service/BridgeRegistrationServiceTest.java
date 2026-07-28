package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.test.service.TestService;

/**
 * OE2 pushes the analyzer's test_code → LOINC map to the bridge at
 * registration, built from the lab's AnalyzerTestMapping (code → testId) joined
 * to Test.loinc. This is what lets the bridge own analyzer↔LOINC translation so
 * OE2 stays analyzer-agnostic.
 */
public class BridgeRegistrationServiceTest {

    private BridgeRegistrationService svc;
    private AnalyzerTestMappingService mappingService;
    private TestService testService;

    @Before
    public void setUp() throws Exception {
        svc = new BridgeRegistrationService();
        mappingService = mock(AnalyzerTestMappingService.class);
        testService = mock(TestService.class);
        inject("analyzerTestMappingService", mappingService);
        inject("testService", testService);
    }

    private void inject(String field, Object value) throws Exception {
        Field f = BridgeRegistrationService.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(svc, value);
    }

    private AnalyzerTestMapping mapping(String code, String testId) {
        AnalyzerTestMapping m = mock(AnalyzerTestMapping.class);
        when(m.getAnalyzerTestName()).thenReturn(code);
        when(m.getTestId()).thenReturn(testId);
        return m;
    }

    private void stubTestLoinc(String testId, String loinc) {
        org.openelisglobal.test.valueholder.Test t = mock(org.openelisglobal.test.valueholder.Test.class);
        when(t.getLoinc()).thenReturn(loinc);
        when(testService.get(testId)).thenReturn(t);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> attachAndGet(String analyzerId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        svc.attachTestCodeLoinc(payload, analyzerId);
        return (Map<String, String>) payload.get("testCodeLoinc");
    }

    @Test
    public void buildsCodeToLoincFromMappingsAndTestLoinc() {
        // Build the mapping mocks BEFORE stubbing getAllForAnalyzer — nesting
        // mock-stubbing inside a thenReturn(...) argument trips Mockito.
        AnalyzerTestMapping m1 = mapping("MTB-RIF", "42");
        AnalyzerTestMapping m2 = mapping("HIV-VL", "43");
        when(mappingService.getAllForAnalyzer("AN-1")).thenReturn(List.of(m1, m2));
        stubTestLoinc("42", "85362-2");
        stubTestLoinc("43", "20447-9");

        Map<String, String> m = attachAndGet("AN-1");

        assertEquals("85362-2", m.get("MTB-RIF"));
        assertEquals("20447-9", m.get("HIV-VL"));
        assertEquals(2, m.size());
    }

    @Test
    public void skipsTestsWithoutLoinc() {
        AnalyzerTestMapping mw = mapping("WBC", "42");
        AnalyzerTestMapping mv = mapping("VENDOR-ONLY", "99");
        when(mappingService.getAllForAnalyzer("AN-2")).thenReturn(List.of(mw, mv));
        stubTestLoinc("42", "6690-2");
        stubTestLoinc("99", null); // no LOINC on this test

        Map<String, String> m = attachAndGet("AN-2");

        assertEquals(1, m.size());
        assertEquals("6690-2", m.get("WBC"));
        assertTrue("vendor-only test with no LOINC is omitted", !m.containsKey("VENDOR-ONLY"));
    }

    @Test
    public void alwaysAttachesEvenWhenEmpty() {
        when(mappingService.getAllForAnalyzer("AN-3")).thenReturn(List.of());
        Map<String, Object> payload = new LinkedHashMap<>();
        svc.attachTestCodeLoinc(payload, "AN-3");
        // Key present (empty) so a sync push can clear stale bridge mappings.
        assertTrue(payload.containsKey("testCodeLoinc"));
        assertNotNull(payload.get("testCodeLoinc"));
    }
}
