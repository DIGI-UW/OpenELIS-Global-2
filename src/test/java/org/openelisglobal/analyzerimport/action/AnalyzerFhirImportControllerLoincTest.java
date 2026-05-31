package org.openelisglobal.analyzerimport.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.test.service.TestService;

/**
 * Analyzer inbound resolves LOINC-coded Observations to OE2 tests by reusing
 * the SAME mechanism OE2 has used to import external FHIR orders for years —
 * TestService.getTestsByLoincCode (see TaskInterpreterImpl.
 * createTestFromFHIR). A non-LOINC coding returns null so the caller falls back
 * to the legacy analyzer-code path (additive, can't regress shipped inbound).
 *
 * Inversion-safe: a hardcoded/null implementation fails the positive case; a
 * "resolve everything" implementation fails the non-LOINC and no-match cases.
 */
public class AnalyzerFhirImportControllerLoincTest {

    private TestService testService;
    private AnalyzerFhirImportController controller;

    @Before
    public void setUp() throws Exception {
        controller = new AnalyzerFhirImportController();
        testService = mock(TestService.class);
        Field f = AnalyzerFhirImportController.class.getDeclaredField("testService");
        f.setAccessible(true);
        f.set(controller, testService);
    }

    private Observation loincObs(String loinc) {
        Observation obs = new Observation();
        obs.setCode(new CodeableConcept().addCoding(new Coding().setSystem("http://loinc.org").setCode(loinc)));
        return obs;
    }

    @Test
    public void loincCodedObservation_resolvesViaGetTestsByLoincCode() {
        org.openelisglobal.test.valueholder.Test t = mock(org.openelisglobal.test.valueholder.Test.class);
        when(t.getId()).thenReturn("501");
        when(testService.getTestsByLoincCode("6690-2")).thenReturn(List.of(t));

        org.openelisglobal.test.valueholder.Test result = controller.resolveLoincTest(loincObs("6690-2"));

        assertNotNull("LOINC-coded observation must resolve to a test", result);
        assertEquals("501", result.getId());
    }

    @Test
    public void nonLoincObservation_returnsNullSoCallerFallsBack() {
        Observation obs = new Observation();
        obs.setCode(new CodeableConcept().addCoding(new Coding().setCode("WBC"))); // raw code, no LOINC system
        assertNull("non-LOINC coding must not resolve here (caller falls back)", controller.resolveLoincTest(obs));
    }

    @Test
    public void loincWithNoMatchingTest_returnsNull() {
        when(testService.getTestsByLoincCode("99999-9")).thenReturn(Collections.emptyList());
        assertNull("unknown LOINC must return null, not throw",
                controller.resolveLoincTest(loincObs("99999-9")));
    }
}
