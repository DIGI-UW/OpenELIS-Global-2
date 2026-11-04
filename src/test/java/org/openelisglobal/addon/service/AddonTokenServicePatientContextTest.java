package org.openelisglobal.addon.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The patient results page names the patient; the server, not the page, decides
 * which lab numbers go into the add-on's context.
 */
public class AddonTokenServicePatientContextTest {

    private final AddonTokenServiceImpl service = new AddonTokenServiceImpl();
    private final SampleService samples = mock(SampleService.class);

    private static Sample sample(String accession, String received) {
        Sample s = new Sample();
        s.setAccessionNumber(accession);
        s.setReceivedTimestamp(received == null ? null : Timestamp.valueOf(received));
        return s;
    }

    @Before
    public void wire() {
        ReflectionTestUtils.setField(service, "sampleService", samples);
    }

    @Test
    public void thePatientsOrdersReplaceWhatThePageSentNewestFirst() {
        when(samples.getSamplesForPatient("42")).thenReturn(List.of(sample("OLD1", "2026-01-01 08:00:00"),
                sample("NEW1", "2026-10-01 08:00:00"), sample("NOREC", null)));
        Map<String, ?> out = service.withPatientOrders(
                Map.of("page", "patient-results", "patientId", "42", "accessionNumbers", List.of("INJECTED")));
        assertEquals(List.of("NEW1", "OLD1", "NOREC"), out.get("accessionNumbers"));
        assertFalse("the patient id is not passed on", out.containsKey("patientId"));
        assertEquals("patient-results", out.get("page"));
    }

    @Test
    public void aMalformedPatientIdGivesNoOrders() {
        Map<String, ?> out = service.withPatientOrders(Map.of("patientId", "1 OR 1=1"));
        assertEquals(List.of(), out.get("accessionNumbers"));
        verify(samples, never()).getSamplesForPatient(anyString());
    }

    @Test
    public void aContextWithoutPatientIsUnchanged() {
        Map<String, Object> context = Map.of("page", "result-validation", "accessionNumbers", List.of("A1"));
        assertEquals(context, service.withPatientOrders(context));
    }
}
