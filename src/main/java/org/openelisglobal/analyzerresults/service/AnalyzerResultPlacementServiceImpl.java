package org.openelisglobal.analyzerresults.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzerresults.service.AnalyzerResultPlacement.Candidate;
import org.openelisglobal.analyzerresults.service.AnalyzerResultPlacement.Match;
import org.openelisglobal.analyzerresults.service.AnalyzerResultPlacement.PatientCheck;
import org.openelisglobal.analyzerresults.service.AnalyzerResultPlacement.PatientStatus;
import org.openelisglobal.analyzerresults.service.AnalyzerResultPlacement.State;
import org.openelisglobal.analyzerresults.service.AnalyzerResultPlacement.Tube;
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
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyzerResultPlacementServiceImpl implements AnalyzerResultPlacementService {

    private static final Comparator<String> BY_ID = Comparator.comparing(String::length)
            .thenComparing(Comparator.naturalOrder());

    private final SampleItemService sampleItems;
    private final SampleService samples;
    private final AnalysisService analyses;
    private final TypeOfSampleTestService sampleTypeTests;
    private final IStatusService statuses;
    private final SampleHumanService sampleHumans;
    private final PatientService patients;

    public AnalyzerResultPlacementServiceImpl(SampleItemService sampleItems, SampleService samples,
            AnalysisService analyses, TypeOfSampleTestService sampleTypeTests, IStatusService statuses,
            SampleHumanService sampleHumans, PatientService patients) {
        this.sampleItems = sampleItems;
        this.samples = samples;
        this.analyses = analyses;
        this.sampleTypeTests = sampleTypeTests;
        this.statuses = statuses;
        this.sampleHumans = sampleHumans;
        this.patients = patients;
    }

    @Override
    public AnalyzerResultPlacement place(String instrumentId, String testId) {
        return place(instrumentId, testId, null, null);
    }

    @Override
    public AnalyzerResultPlacement place(String instrumentId, String testId, String instrumentPatientId,
            String instrumentPatientName) {
        String id = instrumentId == null ? "" : instrumentId.trim();
        List<SampleItem> byTube = id.isEmpty() ? List.of() : sampleItems.getSampleItemsByExternalID(id);
        if (byTube.size() > 1) {
            return new AnalyzerResultPlacement(State.MULTI_TUBE, Match.TUBE, null, tubes(byTube), List.of(), null, null,
                    checkPatient(null, instrumentPatientId, instrumentPatientName));
        }
        Sample sample;
        Match match;
        List<SampleItem> scope;
        if (byTube.size() == 1) {
            sample = byTube.get(0).getSample();
            match = Match.TUBE;
            scope = byTube;
        } else {
            sample = id.isEmpty() ? null : samples.getSampleByAccessionNumber(id);
            if (sample == null) {
                return new AnalyzerResultPlacement(State.NEW_SAMPLE, Match.NONE, null, List.of(), List.of(), null, null,
                        checkPatient(null, instrumentPatientId, instrumentPatientName));
            }
            match = Match.ACCESSION;
            scope = sampleItems.getSampleItemsBySampleId(sample.getId());
        }
        List<Tube> orderTubes = tubes(
                match == Match.TUBE ? sampleItems.getSampleItemsBySampleId(sample.getId()) : scope);
        Set<String> scopeIds = scope.stream().map(SampleItem::getId).collect(Collectors.toSet());
        Set<String> excluded = statusIds(AnalysisStatus.Canceled, AnalysisStatus.SampleRejected,
                AnalysisStatus.RejectedByReferenceLab);
        Set<String> awaiting = statusIds(AnalysisStatus.NotStarted, AnalysisStatus.TechnicalRejected,
                AnalysisStatus.BiologistRejected);
        List<Candidate> candidates = analyses.getAnalysesBySampleId(sample.getId()).stream()
                .filter(analysis -> isTestOnTubes(analysis, testId, scopeIds))
                .filter(analysis -> !excluded.contains(analysis.getStatusId()))
                .sorted(Comparator.comparing(Analysis::getId, BY_ID))
                .map(analysis -> new Candidate(analysis.getId(), analysis.getSampleItem().getId(),
                        analysis.getStatusId(), awaiting.contains(analysis.getStatusId())))
                .toList();
        String accession = sample.getAccessionNumber();
        PatientCheck patient = checkPatient(sample, instrumentPatientId, instrumentPatientName);
        if (candidates.size() > 1) {
            return new AnalyzerResultPlacement(State.MULTI_TUBE, match, accession, orderTubes, candidates, null, null,
                    patient);
        }
        if (candidates.size() == 1) {
            Candidate only = candidates.get(0);
            return new AnalyzerResultPlacement(only.awaitingResult() ? State.RESOLVED : State.RETEST_CHOICE, match,
                    accession, orderTubes, candidates, only.sampleItemId(), only.analysisId(), patient);
        }
        List<SampleItem> fitting = fittingTubes(scope, testId);
        State state = fitting.size() == 1 ? State.UNORDERED_ONE_FITS
                : fitting.isEmpty() ? State.UNORDERED_NONE_FIT : State.UNORDERED_MANY_FIT;
        return new AnalyzerResultPlacement(state, match, accession, orderTubes, List.of(),
                fitting.size() == 1 ? fitting.get(0).getId() : null, null, patient);
    }

    @Override
    public String accessionFor(String instrumentId) {
        String id = instrumentId == null ? "" : instrumentId.trim();
        List<SampleItem> byTube = id.isEmpty() ? List.of() : sampleItems.getSampleItemsByExternalID(id);
        if (byTube.size() == 1 && byTube.get(0).getSample() != null
                && byTube.get(0).getSample().getAccessionNumber() != null) {
            return byTube.get(0).getSample().getAccessionNumber();
        }
        return instrumentId;
    }

    /**
     * Compares the instrument's patient ID with the identifiers of the order's
     * patient. Never creates or changes a patient.
     */
    private PatientCheck checkPatient(Sample sample, String instrumentPatientId, String instrumentPatientName) {
        if (instrumentPatientId == null || instrumentPatientId.isBlank()) {
            return PatientCheck.NOT_REPORTED;
        }
        String reported = instrumentPatientId.trim();
        Patient patient = sample == null ? null : sampleHumans.getPatientForSample(sample);
        List<String> identifiers = patient == null ? List.of()
                : Stream.of(patients.getNationalId(patient), patients.getSTNumber(patient),
                        patients.getSubjectNumber(patient), patients.getExternalId(patient))
                        .filter(identifier -> identifier != null && !identifier.isBlank()).map(String::trim).toList();
        if (identifiers.isEmpty()) {
            return new PatientCheck(PatientStatus.NO_ORDER_PATIENT, reported, instrumentPatientName,
                    patient == null ? null : patients.getLastFirstName(patient));
        }
        boolean same = identifiers.stream().anyMatch(reported::equalsIgnoreCase);
        return new PatientCheck(same ? PatientStatus.MATCH : PatientStatus.MISMATCH, reported, instrumentPatientName,
                patients.getLastFirstName(patient));
    }

    private static boolean isTestOnTubes(Analysis analysis, String testId, Set<String> tubeIds) {
        return analysis.getTest() != null && Objects.equals(testId, analysis.getTest().getId())
                && analysis.getSampleItem() != null && tubeIds.contains(analysis.getSampleItem().getId());
    }

    /** A test with no sample-type restriction fits any tube. */
    private List<SampleItem> fittingTubes(List<SampleItem> scope, String testId) {
        Set<String> allowed = sampleTypeTests.getTypeOfSampleTestsForTest(testId).stream()
                .map(TypeOfSampleTest::getTypeOfSampleId).collect(Collectors.toSet());
        return scope.stream().filter(tube -> allowed.isEmpty() || allowed.contains(tube.getTypeOfSampleId())).toList();
    }

    private Set<String> statusIds(AnalysisStatus... wanted) {
        return Stream.of(wanted).map(statuses::getStatusID).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private static List<Tube> tubes(List<SampleItem> items) {
        return items.stream().sorted(Comparator.comparing(SampleItem::getId, BY_ID))
                .map(item -> new Tube(item.getId(), item.getExternalId(), item.getTypeOfSampleId())).toList();
    }
}
