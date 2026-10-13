package org.openelisglobal.microbiology;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.analyzerresults.service.*;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.service.*;
import org.openelisglobal.microbiology.valueholder.MicroCase;

public class MicroIncomingAnalyzerAdapterTest {
    private final MicroCaseDAO cases = mock(MicroCaseDAO.class);
    private final MicroIncomingResultService incoming = mock(MicroIncomingResultService.class);
    private final AnalyzerResultPlacementService placement = mock(AnalyzerResultPlacementService.class);
    private final AnalyzerResultsService staging = mock(AnalyzerResultsService.class);
    private final MicroIncomingAnalyzerAdapter adapter = new MicroIncomingAnalyzerAdapter(cases, incoming, placement,
            staging, mock(MicroIncomingItemDAO.class));

    private AnalyzerResults row(String component) {
        var r = new AnalyzerResults();
        r.setId(component);
        r.setTestId("12");
        r.setSourceConnectionId("connection");
        r.setSourceMessageId("run-1");
        r.setInstrumentSpecimenId("tube");
        r.setInstrumentPatientId("patient");
        r.setComponentId(component);
        r.setResult("12");
        r.setRawResultValue("12 raw");
        r.setInstrumentNote("Keep instrument evidence");
        return r;
    }

    private void match(AnalyzerResultPlacement.PatientStatus patient) {
        when(placement.place("tube","12","patient",null)).thenReturn(new AnalyzerResultPlacement(
            AnalyzerResultPlacement.State.RESOLVED, AnalyzerResultPlacement.Match.TUBE,"LAB-1",List.of(),List.of(),"tube","analysis",
            new AnalyzerResultPlacement.PatientCheck(patient,"patient",null,null)));
        var c = new MicroCase(); c.setId("case"); c.setLabUnitId("unit");
        when(cases.getBySampleItem("tube")).thenReturn(List.of(c));
        var reply = new MicroIncomingForm(); reply.id="incoming"; reply.state="PLACED";
        when(incoming.receive(anyString(),anyString(),anyString(),anyString(),anyString(),anyString(),any(),anyBoolean(),anyString())).thenReturn(reply);
    }

    @Test
    public void matchedComponentsTravelTogetherAndStagingIsDeletedOnlyAfterPersistence() {
        match(AnalyzerResultPlacement.PatientStatus.MATCH);
        adapter.route(List.of(row("one"), row("two")), "actor");
        var payload = ArgumentCaptor.forClass(MicroCaseResultRequestForm.class);
        var order = inOrder(incoming, staging);
        order.verify(incoming).receive(eq("case"), eq("tube"), eq("12"), eq("ANALYZER"), anyString(),
                eq("12 raw | 12 raw"), payload.capture(), eq(true), eq("actor"));
        assertEquals(2, payload.getValue().components.size());
        assertEquals("Keep instrument evidence", payload.getValue().note);
        order.verify(staging).delete("one", "actor");
        order.verify(staging).delete("two", "actor");
    }

    @Test
    public void patientMismatchStaysInSharedReview() {
        match(AnalyzerResultPlacement.PatientStatus.MISMATCH);
        adapter.route(List.of(row("one")), "actor");
        verifyZeroInteractions(incoming, staging);
    }

    @Test
    public void rejectedPersistenceNeverDeletesSourceEvidence() {
        match(AnalyzerResultPlacement.PatientStatus.MATCH);
        when(incoming.receive(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(),
                anyBoolean(), anyString())).thenThrow(new IllegalArgumentException("invalid components"));
        try {
            adapter.route(List.of(row("one")), "actor");
            fail("Must propagate transaction failure");
        } catch (IllegalArgumentException expected) {
            assertEquals("invalid components", expected.getMessage());
        }
        verifyZeroInteractions(staging);
    }
}
