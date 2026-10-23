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
    private final MicroCaseAnalysisDAO links = mock(MicroCaseAnalysisDAO.class);
    private final MicroIncomingResultService incoming = mock(MicroIncomingResultService.class);
    private final AnalyzerResultPlacementService placement = mock(AnalyzerResultPlacementService.class);
    private final AnalyzerResultsService staging = mock(AnalyzerResultsService.class);
    private final MicroIncomingAnalyzerAdapter adapter = new MicroIncomingAnalyzerAdapter(cases, incoming, placement,
            staging, links);

    private Set<String> accept(List<AnalyzerResults> rows, String actor) {
        return adapter.acceptReviewed(rows.stream().map(this::decision).toList(), actor);
    }

    private org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem decision(AnalyzerResults row) {
        when(staging.get(row.getId())).thenReturn(row);
        var item = new org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem();
        item.setId(row.getId());
        item.setIsAccepted(true);
        item.setResult(row.getResult());
        item.setInstrumentSpecimenId(row.getInstrumentSpecimenId());
        item.setAccessionNumber(row.getAccessionNumber());
        return item;
    }

    @Test
    public void unacceptedRowsNeverTransferOrDeleteStaging() {
        var item = decision(row("one"));
        item.setIsAccepted(false);
        assertTrue(adapter.acceptReviewed(List.of(item), "reviewer").isEmpty());
        verifyZeroInteractions(incoming, staging, cases, links, placement);
    }

    @Test
    public void reviewerEditsAndAcknowledgementsTravelWithTheAcceptedResult() {
        match(AnalyzerResultPlacement.PatientStatus.MATCH);
        var item = decision(row("one"));
        item.setResult("18");
        item.setAnalyzerId("client-supplied");
        item.setNote("Reviewed value");
        item.setCriticalAcknowledged(true);
        item.setInvalidResultConfirmed(true);
        assertEquals(Set.of("one"), adapter.acceptReviewed(List.of(item), "reviewer"));
        var payload = ArgumentCaptor.forClass(MicroCaseResultRequestForm.class);
        verify(incoming).receive(eq("case"), eq("tube"), eq("12"), eq("ANALYZER"), anyString(), anyString(),
                payload.capture(), eq(true), eq("reviewer"), eq("77"));
        assertEquals("18", payload.getValue().components.get(0).value);
        assertTrue(payload.getValue().components.get(0).criticalAcknowledged);
        assertTrue(payload.getValue().components.get(0).invalidResultConfirmed);
        assertEquals("Keep instrument evidence\nReviewed value", payload.getValue().note);
    }

    @Test
    public void conflictingReviewedDestinationsDoNotSaveOrDeleteAnyComponent() {
        match(AnalyzerResultPlacement.PatientStatus.MATCH);
        var first = decision(row("one"));
        var second = decision(row("two"));
        first.setChosenAnalysisId("analysis");
        second.setChosenAnalysisId("another-analysis");
        try {
            adapter.acceptReviewed(List.of(first, second), "reviewer");
            fail("Conflicting component destinations must remain staged");
        } catch (IllegalArgumentException expected) {
            assertEquals("MICROBIOLOGY_ANALYZER_COMPONENT_PLACEMENT_CONFLICT", expected.getMessage());
        }
        verifyZeroInteractions(incoming);
        verify(staging, never()).delete(anyString(), anyString());
    }

    @Test
    public void explicitAndAutomaticChoicesForTheSameAnalysisRemainCompatible() {
        match(AnalyzerResultPlacement.PatientStatus.MATCH);
        var first = decision(row("one"));
        var second = decision(row("two"));
        first.setChosenAnalysisId("analysis");
        assertEquals(Set.of("one", "two"), adapter.acceptReviewed(List.of(first, second), "reviewer"));
    }

    private AnalyzerResults row(String component) {
        var r = new AnalyzerResults();
        r.setId(component);
        r.setTestId("12");
        r.setSourceConnectionId("connection");
        r.setAnalyzerId("77");
        r.setSourceMessageId("run-1");
        r.setInstrumentSpecimenId("tube");
        r.setInstrumentPatientId("patient");
        r.setComponentId(component);
        r.setRawTestCode("raw-" + component);
        r.setRawSubIdentity("sub-" + component);
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
        var owner = new org.openelisglobal.microbiology.valueholder.MicroCaseAnalysis();
        owner.setCaseId("case"); owner.setAnalysisId("analysis");
        when(links.getActiveByAnalysisId("analysis")).thenReturn(owner);
        var reply = new MicroIncomingForm(); reply.id="incoming"; reply.state="PLACED";
        when(incoming.receive(anyString(),anyString(),anyString(),anyString(),anyString(),anyString(),any(),anyBoolean(),anyString(), eq("77"))).thenReturn(reply);
    }

    @Test
    public void matchedComponentsTravelTogetherAndStagingIsDeletedOnlyAfterPersistence() {
        match(AnalyzerResultPlacement.PatientStatus.MATCH);
        accept(List.of(row("one"), row("two")), "actor");
        var payload = ArgumentCaptor.forClass(MicroCaseResultRequestForm.class);
        var order = inOrder(incoming, staging);
        order.verify(incoming).receive(eq("case"), eq("tube"), eq("12"), eq("ANALYZER"), eq("connection:run-1:tube:12"),
                eq("12 raw | 12 raw"), payload.capture(), eq(true), eq("actor"), eq("77"));
        assertEquals(2, payload.getValue().components.size());
        assertEquals("Keep instrument evidence", payload.getValue().note);
        order.verify(staging).delete("one", "actor");
        order.verify(staging).delete("two", "actor");
    }

    @Test
    public void existingAnalysisSelectsItsOwnerAmongRelatedCasesSharingTheSpecimen() {
        match(AnalyzerResultPlacement.PatientStatus.MATCH);
        var owner = new MicroCase();
        owner.setId("case");
        owner.setLabUnitId("unit");
        var related = new MicroCase();
        related.setId("related");
        related.setLabUnitId("other-unit");
        when(cases.getBySampleItem("tube")).thenReturn(List.of(related, owner));
        accept(List.of(row("one")), "actor");
        verify(incoming).receive(eq("case"), eq("tube"), eq("12"), eq("ANALYZER"), anyString(), anyString(), any(),
                eq(true), eq("actor"), eq("77"));
        verify(staging).delete("one", "actor");
    }

    @Test
    public void existingAnalysisWithoutActiveOwnershipStaysInSharedReview() {
        match(AnalyzerResultPlacement.PatientStatus.MATCH);
        when(links.getActiveByAnalysisId("analysis")).thenReturn(null);
        accept(List.of(row("one")), "actor");
        verifyZeroInteractions(incoming);
        verify(staging, never()).delete(anyString(), anyString());
    }

    @Test
    public void unorderedTestWithMultipleCaseOwnersStaysInSharedReview() {
        match(AnalyzerResultPlacement.PatientStatus.MATCH);
        when(placement.place("tube", "12", "patient", null))
                .thenReturn(new AnalyzerResultPlacement(AnalyzerResultPlacement.State.UNORDERED_ONE_FITS,
                        AnalyzerResultPlacement.Match.TUBE, "LAB-1", List.of(), List.of(), "tube", null,
                        new AnalyzerResultPlacement.PatientCheck(AnalyzerResultPlacement.PatientStatus.MATCH, "patient",
                                null, null)));
        var first = new MicroCase();
        first.setId("case");
        first.setLabUnitId("unit");
        var second = new MicroCase();
        second.setId("related");
        second.setLabUnitId("other-unit");
        when(cases.getBySampleItem("tube")).thenReturn(List.of(first, second));
        accept(List.of(row("one")), "actor");
        verifyZeroInteractions(incoming, links);
        verify(staging, never()).delete(anyString(), anyString());
    }

    @Test
    public void patientMismatchStaysInSharedReview() {
        match(AnalyzerResultPlacement.PatientStatus.MISMATCH);
        accept(List.of(row("one")), "actor");
        verifyZeroInteractions(incoming);
        verify(staging, never()).delete(anyString(), anyString());
    }

    @Test
    public void rejectedPersistenceNeverDeletesSourceEvidence() {
        match(AnalyzerResultPlacement.PatientStatus.MATCH);
        when(incoming.receive(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(),
                anyBoolean(), anyString(), eq("77"))).thenThrow(new IllegalArgumentException("invalid components"));
        try {
            accept(List.of(row("one")), "actor");
            fail("Must propagate transaction failure");
        } catch (IllegalArgumentException expected) {
            assertEquals("invalid components", expected.getMessage());
        }
        verify(staging, never()).delete(anyString(), anyString());
    }
}
