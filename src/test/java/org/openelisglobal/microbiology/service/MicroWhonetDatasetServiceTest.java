package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstReadingDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.dao.MicroWhonetContext;
import org.openelisglobal.microbiology.form.MicroWhonetExportQueryForm;
import org.openelisglobal.microbiology.form.MicroWhonetPreviewForm;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunStatus;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseFinalReleaseState;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;

@RunWith(MockitoJUnitRunner.class)
public class MicroWhonetDatasetServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;
    @Mock
    private MicroIsolateDAO isolateDAO;
    @Mock
    private MicroAstRunDAO astRunDAO;
    @Mock
    private MicroAstReadingDAO astReadingDAO;
    @Mock
    private MicroOrganismDAO organismDAO;
    @Mock
    private MicroAntibioticDAO antibioticDAO;
    private final Map<String, MicroWhonetContext> contextsByCase = new HashMap<>();
    private MicroWhonetDatasetService service;

    @Before
    public void setUp() {
        service = new MicroWhonetDatasetServiceImpl(caseDAO, isolateDAO, astRunDAO, astReadingDAO, organismDAO,
                antibioticDAO);
        when(caseDAO.getWhonetContextsByCaseIds(any())).thenAnswer(invocation -> {
            List<String> caseIds = invocation.getArgument(0);
            return caseIds.stream().map(contextsByCase::get).filter(context -> context != null).toList();
        });
    }

    @Test
    public void previewIncludesEveryMappedReadingFromTheReportableReviewedRun() {
        MicroCase microCase = finalizedCase("case-1", "item-1", "2026-07-12 10:00:00");
        MicroIsolate isolate = isolate("isolate-1", "case-1", "organism-1");
        MicroAstRun run = reviewedRun("run-1", "isolate-1");
        MicroAstReading cip = reading("reading-1", "run-1", "antibiotic-1", "SUSCEPTIBLE");
        MicroAstReading gen = reading("reading-2", "run-1", "antibiotic-2", "RESISTANT");

        when(caseDAO.getFinalizedBacteriologyByClosedAtRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(microCase));
        when(isolateDAO.getByCaseIds(List.of("case-1"))).thenReturn(List.of(isolate));
        when(astRunDAO.getByIsolateIds(List.of("isolate-1"))).thenReturn(List.of(run));
        when(astReadingDAO.getByRunIds(List.of("run-1"))).thenReturn(List.of(cip, gen));
        when(organismDAO.get("organism-1")).thenReturn(Optional.of(organism("organism-1", "eco", "E. coli")));
        when(antibioticDAO.get("antibiotic-1"))
                .thenReturn(Optional.of(antibiotic("antibiotic-1", "CIP", "Ciprofloxacin")));
        when(antibioticDAO.get("antibiotic-2"))
                .thenReturn(Optional.of(antibiotic("antibiotic-2", "GEN", "Gentamicin")));
        stubPatientContext("case-1", "item-1", "patient-1", "LAB-001");

        MicroWhonetPreviewForm preview = service.compile(query("NONE")).getPreview();

        assertEquals(1, preview.totalCases);
        assertEquals(1, preview.totalIsolates);
        assertEquals(1, preview.afterSignificance);
        assertEquals(1, preview.afterDeduplication);
        assertEquals(1, preview.exportableIsolates);
        assertEquals(2, preview.exportedRows);
        assertEquals(2, preview.rows.size());
        assertEquals("CIP", preview.rows.get(0).antibioticCode);
        assertEquals("S", preview.rows.get(0).interpretation);
        assertEquals("GEN", preview.rows.get(1).antibioticCode);
        assertEquals("R", preview.rows.get(1).interpretation);
        assertTrue(preview.canGenerate);
        assertTrue(preview.warnings.isEmpty());
    }

    @Test
    public void previewExcludesOnlyUnmappedReadingsAndProvidesExactRepairTarget() {
        MicroCase microCase = finalizedCase("case-1", "item-1", "2026-07-12 10:00:00");
        MicroIsolate isolate = isolate("isolate-1", "case-1", "organism-1");
        MicroAstRun run = reviewedRun("run-1", "isolate-1");

        when(caseDAO.getFinalizedBacteriologyByClosedAtRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(microCase));
        when(isolateDAO.getByCaseIds(List.of("case-1"))).thenReturn(List.of(isolate));
        when(astRunDAO.getByIsolateIds(List.of("isolate-1"))).thenReturn(List.of(run));
        when(astReadingDAO.getByRunIds(List.of("run-1")))
                .thenReturn(List.of(reading("reading-1", "run-1", "antibiotic-1", "S"),
                        reading("reading-2", "run-1", "antibiotic-2", "R")));
        when(organismDAO.get("organism-1")).thenReturn(Optional.of(organism("organism-1", "eco", "E. coli")));
        when(antibioticDAO.get("antibiotic-1"))
                .thenReturn(Optional.of(antibiotic("antibiotic-1", "CIP", "Ciprofloxacin")));
        when(antibioticDAO.get("antibiotic-2")).thenReturn(Optional.of(antibiotic("antibiotic-2", "", "Gentamicin")));
        stubPatientContext("case-1", "item-1", "patient-1", "LAB-001");

        MicroWhonetPreviewForm preview = service.compile(query("NONE")).getPreview();

        assertTrue(preview.canGenerate);
        assertEquals(1, preview.exportedRows);
        assertEquals(1, preview.excludedRows);
        assertEquals(1, preview.warnings.size());
        assertEquals("ANTIBIOTIC_MAPPING_REQUIRED", preview.warnings.get(0).code);
        assertEquals("antibiotics", preview.warnings.get(0).resource);
        assertEquals("antibiotic-2", preview.warnings.get(0).resourceId);
    }

    @Test
    public void previewBlocksGenerationWhenEveryCandidateHasAnUnmappedOrganism() {
        MicroCase microCase = finalizedCase("case-1", "item-1", "2026-07-12 10:00:00");
        MicroIsolate isolate = isolate("isolate-1", "case-1", "organism-1");
        MicroAstRun run = reviewedRun("run-1", "isolate-1");

        when(caseDAO.getFinalizedBacteriologyByClosedAtRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(microCase));
        when(isolateDAO.getByCaseIds(List.of("case-1"))).thenReturn(List.of(isolate));
        when(astRunDAO.getByIsolateIds(List.of("isolate-1"))).thenReturn(List.of(run));
        when(astReadingDAO.getByRunIds(List.of("run-1")))
                .thenReturn(List.of(reading("reading-1", "run-1", "antibiotic-1", "S")));
        when(organismDAO.get("organism-1")).thenReturn(Optional.of(organism("organism-1", "", "E. coli")));
        stubPatientContext("case-1", "item-1", "patient-1", "LAB-001");

        MicroWhonetPreviewForm preview = service.compile(query("NONE")).getPreview();

        assertFalse(preview.canGenerate);
        assertEquals(0, preview.exportedRows);
        assertEquals(1, preview.excludedRows);
        assertEquals(1, preview.warnings.size());
        assertEquals("ORGANISM_MAPPING_REQUIRED", preview.warnings.get(0).code);
        assertEquals("organism-1", preview.warnings.get(0).resourceId);
        verify(antibioticDAO, never()).get("antibiotic-1");
    }

    @Test
    public void previewExcludesReadingsWithoutAWhonetSirInterpretation() {
        MicroCase microCase = finalizedCase("case-1", "item-1", "2026-07-12 10:00:00");
        MicroIsolate isolate = isolate("isolate-1", "case-1", "organism-1");
        MicroAstRun run = reviewedRun("run-1", "isolate-1");

        when(caseDAO.getFinalizedBacteriologyByClosedAtRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(microCase));
        when(isolateDAO.getByCaseIds(List.of("case-1"))).thenReturn(List.of(isolate));
        when(astRunDAO.getByIsolateIds(List.of("isolate-1"))).thenReturn(List.of(run));
        when(astReadingDAO.getByRunIds(List.of("run-1")))
                .thenReturn(List.of(reading("reading-1", "run-1", "antibiotic-1", "NO_BREAKPOINT")));
        when(organismDAO.get("organism-1")).thenReturn(Optional.of(organism("organism-1", "eco", "E. coli")));
        when(antibioticDAO.get("antibiotic-1"))
                .thenReturn(Optional.of(antibiotic("antibiotic-1", "CIP", "Ciprofloxacin")));
        stubPatientContext("case-1", "item-1", "patient-1", "LAB-001");

        MicroWhonetPreviewForm preview = service.compile(query("NONE")).getPreview();

        assertFalse(preview.canGenerate);
        assertEquals(0, preview.exportedRows);
        assertEquals(1, preview.excludedRows);
        assertEquals("AST_INTERPRETATION_REQUIRED", preview.warnings.get(0).code);
    }

    @Test
    public void previewIncludesOnlyClinicallySignificantIsolatesByDefault() {
        MicroCase microCase = finalizedCase("case-1", "item-1", "2026-07-12 10:00:00");
        MicroIsolate clinical = isolate("isolate-1", "case-1", "organism-1");
        MicroIsolate colonizer = isolate("isolate-2", "case-1", "organism-1");
        colonizer.setSignificance("COLONIZER");
        MicroAstRun run = reviewedRun("run-1", "isolate-1");

        when(caseDAO.getFinalizedBacteriologyByClosedAtRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(microCase));
        when(isolateDAO.getByCaseIds(List.of("case-1"))).thenReturn(List.of(clinical, colonizer));
        when(astRunDAO.getByIsolateIds(List.of("isolate-1"))).thenReturn(List.of(run));
        when(astReadingDAO.getByRunIds(List.of("run-1")))
                .thenReturn(List.of(reading("reading-1", "run-1", "antibiotic-1", "S")));
        when(organismDAO.get("organism-1")).thenReturn(Optional.of(organism("organism-1", "eco", "E. coli")));
        when(antibioticDAO.get("antibiotic-1"))
                .thenReturn(Optional.of(antibiotic("antibiotic-1", "CIP", "Ciprofloxacin")));
        stubPatientContext("case-1", "item-1", "patient-1", "LAB-001");

        MicroWhonetPreviewForm preview = service.compile(query("NONE")).getPreview();

        assertEquals(2, preview.totalIsolates);
        assertEquals(1, preview.afterSignificance);
        assertEquals(1, preview.exportableIsolates);
        assertEquals(1, preview.exportedRows);
    }

    @Test
    public void previewUsesOnlyReviewedAndExplicitlyReportableAstRuns() {
        MicroCase microCase = finalizedCase("case-1", "item-1", "2026-07-12 10:00:00");
        MicroIsolate isolate = isolate("isolate-1", "case-1", "organism-1");
        MicroAstRun selected = reviewedRun("run-selected", "isolate-1");
        MicroAstRun unreviewed = reviewedRun("run-unreviewed", "isolate-1");
        unreviewed.setStatus(MicroAstRunStatus.IN_PROGRESS.name());
        MicroAstRun notReportable = reviewedRun("run-not-reportable", "isolate-1");
        notReportable.setReportable(false);

        when(caseDAO.getFinalizedBacteriologyByClosedAtRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(microCase));
        when(isolateDAO.getByCaseIds(List.of("case-1"))).thenReturn(List.of(isolate));
        when(astRunDAO.getByIsolateIds(List.of("isolate-1"))).thenReturn(List.of(selected, unreviewed, notReportable));
        when(astReadingDAO.getByRunIds(List.of("run-selected")))
                .thenReturn(List.of(reading("reading-1", "run-selected", "antibiotic-1", "S")));
        when(organismDAO.get("organism-1")).thenReturn(Optional.of(organism("organism-1", "eco", "E. coli")));
        when(antibioticDAO.get("antibiotic-1"))
                .thenReturn(Optional.of(antibiotic("antibiotic-1", "CIP", "Ciprofloxacin")));
        stubPatientContext("case-1", "item-1", "patient-1", "LAB-001");

        MicroWhonetPreviewForm preview = service.compile(query("NONE")).getPreview();

        verify(astReadingDAO).getByRunIds(List.of("run-selected"));
        assertEquals(1, preview.exportedRows);
        assertEquals("S", preview.rows.get(0).interpretation);
    }

    @Test
    public void sevenDayFirstIsolateKeepsTheEarlierPatientOrganismResult() {
        MicroCase firstCase = finalizedCase("case-1", "item-1", "2026-07-10 10:00:00");
        MicroCase repeatCase = finalizedCase("case-2", "item-2", "2026-07-14 10:00:00");
        MicroIsolate first = isolate("isolate-1", "case-1", "organism-1");
        MicroIsolate repeat = isolate("isolate-2", "case-2", "organism-1");
        MicroAstRun firstRun = reviewedRun("run-1", "isolate-1");

        when(caseDAO.getFinalizedBacteriologyByClosedAtRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(repeatCase, firstCase));
        when(isolateDAO.getByCaseIds(List.of("case-2", "case-1"))).thenReturn(List.of(repeat, first));
        when(astRunDAO.getByIsolateIds(List.of("isolate-1"))).thenReturn(List.of(firstRun));
        when(astReadingDAO.getByRunIds(List.of("run-1")))
                .thenReturn(List.of(reading("reading-1", "run-1", "antibiotic-1", "S")));
        when(organismDAO.get("organism-1")).thenReturn(Optional.of(organism("organism-1", "eco", "E. coli")));
        when(antibioticDAO.get("antibiotic-1"))
                .thenReturn(Optional.of(antibiotic("antibiotic-1", "CIP", "Ciprofloxacin")));
        stubPatientContext("case-1", "item-1", "patient-1", "LAB-001");
        stubPatientContext("case-2", "item-2", "patient-1", "LAB-002");

        MicroWhonetPreviewForm preview = service.compile(query("FIRST_ISOLATE_7_DAY")).getPreview();

        assertEquals(2, preview.afterSignificance);
        assertEquals(1, preview.afterDeduplication);
        assertEquals(1, preview.exportedRows);
        assertEquals("case-1", preview.rows.get(0).caseId);
        assertEquals("S", preview.rows.get(0).interpretation);
        assertFalse(preview.rows.stream().anyMatch(row -> "case-2".equals(row.caseId)));
    }

    @Test
    public void compileDoesNotLoadPatientAndSpecimenContextOneCaseAtATime() {
        MicroCase firstCase = finalizedCase("case-1", "item-1", "2026-07-10 10:00:00");
        MicroCase secondCase = finalizedCase("case-2", "item-2", "2026-07-11 10:00:00");
        when(caseDAO.getFinalizedBacteriologyByClosedAtRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(firstCase, secondCase));
        when(isolateDAO.getByCaseIds(List.of("case-1", "case-2"))).thenReturn(List.of());

        service.compile(query("NONE"));

        verify(caseDAO).getWhonetContextsByCaseIds(List.of("case-1", "case-2"));
    }

    @Test
    public void sevenDayBoundaryIncludesAnIsolateAtExactlySevenDays() {
        MicroCase firstCase = finalizedCase("case-1", "item-1", "2026-07-10 10:00:00");
        MicroCase insideWindow = finalizedCase("case-2", "item-2", "2026-07-17 09:59:59");
        MicroCase boundaryCase = finalizedCase("case-3", "item-3", "2026-07-17 10:00:00");
        MicroIsolate first = isolate("isolate-1", "case-1", "organism-1");
        MicroIsolate inside = isolate("isolate-2", "case-2", "organism-1");
        MicroIsolate boundary = isolate("isolate-3", "case-3", "organism-1");
        MicroAstRun firstRun = reviewedRun("run-1", "isolate-1");
        MicroAstRun boundaryRun = reviewedRun("run-3", "isolate-3");

        when(caseDAO.getFinalizedBacteriologyByClosedAtRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(boundaryCase, insideWindow, firstCase));
        when(isolateDAO.getByCaseIds(List.of("case-3", "case-2", "case-1")))
                .thenReturn(List.of(boundary, inside, first));
        when(astRunDAO.getByIsolateIds(List.of("isolate-1", "isolate-3"))).thenReturn(List.of(firstRun, boundaryRun));
        when(astReadingDAO.getByRunIds(List.of("run-1", "run-3")))
                .thenReturn(List.of(reading("reading-1", "run-1", "antibiotic-1", "S"),
                        reading("reading-3", "run-3", "antibiotic-1", "R")));
        when(organismDAO.get("organism-1")).thenReturn(Optional.of(organism("organism-1", "eco", "E. coli")));
        when(antibioticDAO.get("antibiotic-1"))
                .thenReturn(Optional.of(antibiotic("antibiotic-1", "CIP", "Ciprofloxacin")));
        stubPatientContext("case-1", "item-1", "patient-1", "LAB-001");
        stubPatientContext("case-2", "item-2", "patient-1", "LAB-002");
        stubPatientContext("case-3", "item-3", "patient-1", "LAB-003");

        MicroWhonetPreviewForm preview = service.compile(query("FIRST_ISOLATE_7_DAY")).getPreview();

        assertEquals(3, preview.afterSignificance);
        assertEquals(2, preview.afterDeduplication);
        assertEquals(2, preview.exportedRows);
        assertTrue(preview.rows.stream().anyMatch(row -> "case-1".equals(row.caseId)));
        assertFalse(preview.rows.stream().anyMatch(row -> "case-2".equals(row.caseId)));
        assertTrue(preview.rows.stream().anyMatch(row -> "case-3".equals(row.caseId)));
    }

    @Test
    public void dateRangeUsesInclusiveStartAndExclusiveDayAfterEnd() {
        when(caseDAO.getFinalizedBacteriologyByClosedAtRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());

        MicroWhonetPreviewForm preview = service.compile(query("NONE")).getPreview();

        verify(caseDAO).getFinalizedBacteriologyByClosedAtRange(Timestamp.valueOf("2026-07-01 00:00:00"),
                Timestamp.valueOf("2026-08-01 00:00:00"));
        assertEquals("2026-07-01", preview.from);
        assertEquals("2026-07-31", preview.to);
        assertFalse(preview.canGenerate);
    }

    private MicroWhonetExportQueryForm query(String dedup) {
        MicroWhonetExportQueryForm query = new MicroWhonetExportQueryForm();
        query.from = LocalDate.of(2026, 7, 1).toString();
        query.to = LocalDate.of(2026, 7, 31).toString();
        query.significance = MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name();
        query.dedup = dedup;
        query.page = 1;
        query.pageSize = 20;
        return query;
    }

    private MicroCase finalizedCase(String id, String sampleItemId, String closedAt) {
        MicroCase microCase = new MicroCase();
        microCase.setId(id);
        microCase.setSampleItemId(sampleItemId);
        microCase.setWorkflowType("BACTERIOLOGY");
        microCase.setFinalReleaseState(MicroCaseFinalReleaseState.FINAL_RELEASED.name());
        microCase.setClosedAt(Timestamp.valueOf(closedAt));
        return microCase;
    }

    private MicroIsolate isolate(String id, String caseId, String organismId) {
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId(id);
        isolate.setCaseId(caseId);
        isolate.setOrganismId(organismId);
        isolate.setIsolateLabel("ISO-1");
        isolate.setSignificance(MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name());
        return isolate;
    }

    private MicroAstRun reviewedRun(String id, String isolateId) {
        MicroAstRun run = new MicroAstRun();
        run.setId(id);
        run.setIsolateId(isolateId);
        run.setStatus(MicroAstRunStatus.REVIEWED.name());
        run.setReportable(true);
        return run;
    }

    private MicroAstReading reading(String id, String runId, String antibioticId, String interpretation) {
        MicroAstReading reading = new MicroAstReading();
        reading.setId(id);
        reading.setAstRunId(runId);
        reading.setAntibioticId(antibioticId);
        reading.setMethod("MIC");
        reading.setRawValue(new BigDecimal("1.0"));
        reading.setInterpretation(interpretation);
        return reading;
    }

    private MicroOrganism organism(String id, String code, String name) {
        MicroOrganism organism = new MicroOrganism();
        organism.setId(id);
        organism.setWhonetCode(code);
        organism.setDisplayName(name);
        return organism;
    }

    private MicroAntibiotic antibiotic(String id, String code, String name) {
        MicroAntibiotic antibiotic = new MicroAntibiotic();
        antibiotic.setId(id);
        antibiotic.setWhonetCode(code);
        antibiotic.setDisplayName(name);
        return antibiotic;
    }

    private void stubPatientContext(String caseId, String sampleItemId, String patientId, String accession) {
        contextsByCase.put(caseId, new MicroWhonetContext(caseId, sampleItemId, patientId, "NAT-001", "Ada", "Lovelace",
                "F", null, accession, null, Timestamp.valueOf("2026-07-09 09:00:00"), "Blood", null, null));
    }
}
