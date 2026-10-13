package org.openelisglobal.analyzerresults.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzerresults.service.AnalyzerResultPlacement.Match;
import org.openelisglobal.analyzerresults.service.AnalyzerResultPlacement.PatientStatus;
import org.openelisglobal.analyzerresults.service.AnalyzerResultPlacement.State;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;

public class AnalyzerResultPlacementServiceTest {
    private static final String ACCESSION = "26000000001";
    private static final String TEST = "T1";
    private static final Map<AnalysisStatus, String> STATUS = Map.of(AnalysisStatus.NotStarted, "1",
            AnalysisStatus.Canceled, "2", AnalysisStatus.TechnicalAcceptance, "3", AnalysisStatus.TechnicalRejected,
            "4", AnalysisStatus.BiologistRejected, "5", AnalysisStatus.Finalized, "6", AnalysisStatus.SampleRejected,
            "7", AnalysisStatus.RejectedByReferenceLab, "8");

    private final SampleItemService sampleItems = mock(SampleItemService.class);
    private final SampleService samples = mock(SampleService.class);
    private final AnalysisService analyses = mock(AnalysisService.class);
    private final TypeOfSampleTestService sampleTypeTests = mock(TypeOfSampleTestService.class);
    private final IStatusService statuses = mock(IStatusService.class);
    private final SampleHumanService sampleHumans = mock(SampleHumanService.class);
    private final PatientService patients = mock(PatientService.class);
    private final AnalyzerResultPlacementService placement = new AnalyzerResultPlacementServiceImpl(sampleItems,
            samples, analyses, sampleTypeTests, statuses, sampleHumans, patients);

    private final Sample sample = sample("100", ACCESSION);
    private final List<SampleItem> tubes = new ArrayList<>();
    private final List<Analysis> sampleAnalyses = new ArrayList<>();

    @Before
    public void order() {
        STATUS.forEach((status, id) -> when(statuses.getStatusID(status)).thenReturn(id));
        when(samples.getSampleByAccessionNumber(ACCESSION)).thenReturn(sample);
        when(sampleItems.getSampleItemsBySampleId("100")).thenReturn(tubes);
        when(analyses.getAnalysesBySampleId("100")).thenReturn(sampleAnalyses);
        when(sampleTypeTests.getTypeOfSampleTestsForTest(TEST)).thenReturn(List.of());
    }

    @Test
    public void accessionWithOneTubeAndOneAwaitingAnalysisResolves() {
        SampleItem tube = tube("10", ACCESSION + "-1", "SERUM");
        Analysis analysis = analysis("50", tube, AnalysisStatus.NotStarted);

        var result = placement.place(ACCESSION, TEST);

        assertEquals(State.RESOLVED, result.state());
        assertEquals(Match.ACCESSION, result.match());
        assertEquals("50", result.proposedAnalysisId());
        assertEquals("10", result.proposedSampleItemId());
        assertEquals(List.of("50"), result.analyses().stream().map(c -> c.analysisId()).toList());
        assertEquals(analysis.getSampleItem().getId(), result.analyses().get(0).sampleItemId());
    }

    @Test
    public void tubeIdResolvesToThatTubesAnalysis() {
        SampleItem first = tube("10", ACCESSION + "-1", "SERUM");
        SampleItem second = tube("11", ACCESSION + "-2", "PLASMA");
        analysis("50", first, AnalysisStatus.NotStarted);
        analysis("51", second, AnalysisStatus.NotStarted);
        when(sampleItems.getSampleItemsByExternalID(ACCESSION + "-2")).thenReturn(List.of(second));

        var result = placement.place(ACCESSION + "-2", TEST);

        assertEquals(State.RESOLVED, result.state());
        assertEquals(Match.TUBE, result.match());
        assertEquals("51", result.proposedAnalysisId());
        assertEquals("11", result.proposedSampleItemId());
    }

    @Test
    public void sameTestOnTwoTubesIsNotResolvedAndCarriesBothAnalyses() {
        analysis("50", tube("10", ACCESSION + "-1", "SERUM"), AnalysisStatus.NotStarted);
        analysis("51", tube("11", ACCESSION + "-2", "PLASMA"), AnalysisStatus.NotStarted);

        var result = placement.place(ACCESSION, TEST);

        assertEquals(State.MULTI_TUBE, result.state());
        assertNull("never the first match", result.proposedAnalysisId());
        assertNull(result.proposedSampleItemId());
        assertEquals(List.of("50", "51"), result.analyses().stream().map(c -> c.analysisId()).toList());
        assertEquals(2, result.tubes().size());
    }

    @Test
    public void anAnalysisThatAlreadyHoldsAnAcceptedOrFinalResultIsARetestChoice() {
        for (AnalysisStatus held : List.of(AnalysisStatus.TechnicalAcceptance, AnalysisStatus.Finalized)) {
            tubes.clear();
            sampleAnalyses.clear();
            analysis("50", tube("10", ACCESSION + "-1", "SERUM"), held);

            var result = placement.place(ACCESSION, TEST);

            assertEquals(held.name(), State.RETEST_CHOICE, result.state());
            assertEquals("50", result.proposedAnalysisId());
            assertEquals(false, result.analyses().get(0).awaitingResult());
        }
    }

    @Test
    public void anAnalysisAwaitingARerunResolves() {
        for (AnalysisStatus awaiting : List.of(AnalysisStatus.TechnicalRejected, AnalysisStatus.BiologistRejected)) {
            tubes.clear();
            sampleAnalyses.clear();
            analysis("50", tube("10", ACCESSION + "-1", "SERUM"), awaiting);

            var result = placement.place(ACCESSION, TEST);

            assertEquals(awaiting.name(), State.RESOLVED, result.state());
            assertEquals(true, result.analyses().get(0).awaitingResult());
        }
    }

    @Test
    public void cancelledAndRejectedAnalysesAreNeverCandidates() {
        SampleItem tube = tube("10", ACCESSION + "-1", "SERUM");
        analysis("50", tube, AnalysisStatus.Canceled);
        analysis("51", tube, AnalysisStatus.SampleRejected);
        analysis("52", tube, AnalysisStatus.RejectedByReferenceLab);
        analysis("53", tube, AnalysisStatus.NotStarted);

        var result = placement.place(ACCESSION, TEST);

        assertEquals(State.RESOLVED, result.state());
        assertEquals("53", result.proposedAnalysisId());
        assertEquals(1, result.analyses().size());
    }

    @Test
    public void unorderedTestWithOneFittingTubeProposesThatTube() {
        tube("10", ACCESSION + "-1", "SERUM");
        tube("11", ACCESSION + "-2", "PLASMA");
        allowedOn("PLASMA");

        var result = placement.place(ACCESSION, TEST);

        assertEquals(State.UNORDERED_ONE_FITS, result.state());
        assertEquals("11", result.proposedSampleItemId());
        assertNull(result.proposedAnalysisId());
    }

    @Test
    public void unorderedTestWithSeveralFittingTubesProposesNone() {
        tube("10", ACCESSION + "-1", "SERUM");
        tube("11", ACCESSION + "-2", "PLASMA");
        allowedOn("SERUM", "PLASMA");

        var result = placement.place(ACCESSION, TEST);

        assertEquals(State.UNORDERED_MANY_FIT, result.state());
        assertNull(result.proposedSampleItemId());
    }

    @Test
    public void unorderedTestNoTubeCanHoldNeedsANewTube() {
        tube("10", ACCESSION + "-1", "SERUM");
        allowedOn("URINE");

        var result = placement.place(ACCESSION, TEST);

        assertEquals(State.UNORDERED_NONE_FIT, result.state());
        assertNull(result.proposedSampleItemId());
        assertEquals(1, result.tubes().size());
    }

    @Test
    public void unorderedTestOnATubeIdIsJudgedOnThatTubeAlone() {
        SampleItem serum = tube("10", ACCESSION + "-1", "SERUM");
        tube("11", ACCESSION + "-2", "PLASMA");
        allowedOn("PLASMA");
        when(sampleItems.getSampleItemsByExternalID(ACCESSION + "-1")).thenReturn(List.of(serum));

        var result = placement.place(ACCESSION + "-1", TEST);

        assertEquals(State.UNORDERED_NONE_FIT, result.state());
        assertEquals(Match.TUBE, result.match());
    }

    @Test
    public void testWithNoSampleTypeRestrictionFitsASingleTube() {
        tube("10", ACCESSION + "-1", "SERUM");

        var result = placement.place(ACCESSION, TEST);

        assertEquals(State.UNORDERED_ONE_FITS, result.state());
        assertEquals("10", result.proposedSampleItemId());
    }

    @Test
    public void anIdThatMatchesNoTubeOrOrderIsANewSample() {
        var result = placement.place("UNKNOWN-99", TEST);

        assertEquals(State.NEW_SAMPLE, result.state());
        assertEquals(Match.NONE, result.match());
        assertNull(result.accessionNumber());
    }

    @Test
    public void aTubeIdSharedByTwoTubesIsNotResolved() {
        SampleItem first = tube("10", "DUP-1", "SERUM");
        SampleItem second = tube("20", "DUP-1", "SERUM");
        when(sampleItems.getSampleItemsByExternalID("DUP-1")).thenReturn(List.of(first, second));

        var result = placement.place("DUP-1", TEST);

        assertEquals(State.MULTI_TUBE, result.state());
        assertEquals(Match.TUBE, result.match());
        assertNull(result.proposedSampleItemId());
    }

    @Test
    public void aTubeIdResolvesToItsOrdersAccession() {
        SampleItem tube = tube("10", ACCESSION + "-1", "SERUM");
        when(sampleItems.getSampleItemsByExternalID(ACCESSION + "-1")).thenReturn(List.of(tube));

        assertEquals(ACCESSION, placement.accessionFor(ACCESSION + "-1"));
    }

    @Test
    public void anIdThatNamesNoUniqueTubeIsItsOwnAccession() {
        SampleItem first = tube("10", "DUP-1", "SERUM");
        SampleItem second = tube("20", "DUP-1", "SERUM");
        when(sampleItems.getSampleItemsByExternalID("DUP-1")).thenReturn(List.of(first, second));

        assertEquals("DUP-1", placement.accessionFor("DUP-1"));
        assertEquals(ACCESSION, placement.accessionFor(ACCESSION));
        assertNull(placement.accessionFor(null));
    }

    @Test
    public void noReportedPatientIsNotComparedAtAll() {
        tube("10", ACCESSION + "-1", "SERUM");

        var result = placement.place(ACCESSION, TEST);

        assertEquals(PatientStatus.NOT_REPORTED, result.patient().status());
    }

    @Test
    public void aReportedIdThatIsOneOfTheOrderPatientsIdentifiersMatches() {
        tube("10", ACCESSION + "-1", "SERUM");
        orderPatient("N-1", "ST-1", "SUBJ-1", "EXT-1", "Doe, Jane");

        for (String reported : List.of("N-1", " st-1 ", "subj-1", "EXT-1")) {
            var result = placement.place(ACCESSION, TEST, reported, "Reported Name");
            assertEquals(reported, PatientStatus.MATCH, result.patient().status());
        }
    }

    @Test
    public void aReportedIdThatIsNoneOfTheirIdentifiersIsAMismatchBothSidesCanSee() {
        tube("10", ACCESSION + "-1", "SERUM");
        orderPatient("N-1", null, null, null, "Doe, Jane");

        var result = placement.place(ACCESSION, TEST, "OTHER-9", "Roe, Rick");

        assertEquals(PatientStatus.MISMATCH, result.patient().status());
        assertEquals("OTHER-9", result.patient().instrumentId());
        assertEquals("Roe, Rick", result.patient().instrumentName());
        assertEquals("Doe, Jane", result.patient().orderName());
    }

    @Test
    public void anOrderPatientWithNoIdentifierCannotBeVerified() {
        tube("10", ACCESSION + "-1", "SERUM");
        orderPatient(null, null, null, null, "UNKNOWN");

        var result = placement.place(ACCESSION, TEST, "PAT-77", null);

        assertEquals(PatientStatus.NO_ORDER_PATIENT, result.patient().status());
        assertEquals("PAT-77", result.patient().instrumentId());
    }

    @Test
    public void anUnregisteredSpecimenHasNoPatientToCompareWith() {
        var result = placement.place("UNKNOWN-99", TEST, "PAT-77", "Doe, Jane");

        assertEquals(State.NEW_SAMPLE, result.state());
        assertEquals(PatientStatus.NO_ORDER_PATIENT, result.patient().status());
    }

    private void orderPatient(String national, String st, String subject, String external, String name) {
        Patient patient = new Patient();
        when(sampleHumans.getPatientForSample(sample)).thenReturn(patient);
        when(patients.getNationalId(patient)).thenReturn(national);
        when(patients.getSTNumber(patient)).thenReturn(st);
        when(patients.getSubjectNumber(patient)).thenReturn(subject);
        when(patients.getExternalId(patient)).thenReturn(external);
        when(patients.getLastFirstName(patient)).thenReturn(name);
    }

    private void allowedOn(String... sampleTypeIds) {
        List<TypeOfSampleTest> links = new ArrayList<>();
        for (String id : sampleTypeIds) {
            TypeOfSampleTest link = new TypeOfSampleTest();
            link.setTypeOfSampleId(id);
            link.setTestId(TEST);
            links.add(link);
        }
        when(sampleTypeTests.getTypeOfSampleTestsForTest(TEST)).thenReturn(links);
    }

    private static Sample sample(String id, String accession) {
        Sample value = new Sample();
        value.setId(id);
        value.setAccessionNumber(accession);
        return value;
    }

    private SampleItem tube(String id, String externalId, String typeId) {
        SampleItem tube = new SampleItem();
        tube.setId(id);
        tube.setExternalId(externalId);
        tube.setSample(sample);
        TypeOfSample type = new TypeOfSample();
        type.setId(typeId);
        tube.setTypeOfSample(type);
        tubes.add(tube);
        return tube;
    }

    private Analysis analysis(String id, SampleItem tube, AnalysisStatus status) {
        Analysis analysis = new Analysis();
        analysis.setId(id);
        analysis.setSampleItem(tube);
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId(TEST);
        analysis.setTest(test);
        analysis.setStatusId(STATUS.get(status));
        sampleAnalyses.add(analysis);
        return analysis;
    }
}
