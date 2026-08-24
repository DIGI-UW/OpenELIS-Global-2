package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
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
import org.openelisglobal.microbiology.dao.MicroCaseOrderDetailDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.dao.MicroPatientOriginDAO;
import org.openelisglobal.microbiology.dao.MicroWorklistContextDAO;
import org.openelisglobal.microbiology.form.MicroWhonetExportQueryForm;
import org.openelisglobal.microbiology.form.MicroWhonetFilterOptionsForm;
import org.openelisglobal.microbiology.form.MicroWhonetPatientContext;
import org.openelisglobal.microbiology.form.MicroWhonetPreviewForm;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunStatus;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseFinalReleaseState;
import org.openelisglobal.microbiology.valueholder.MicroCaseOrderDetail;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.microbiology.valueholder.MicroPatientOrigin;

@RunWith(MockitoJUnitRunner.class)
public class MicroWhonetDatasetServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;
    @Mock
    private MicroCaseOrderDetailDAO caseOrderDetailDAO;
    @Mock
    private MicroIsolateDAO isolateDAO;
    @Mock
    private MicroAstRunDAO astRunDAO;
    @Mock
    private MicroAstReadingDAO astReadingDAO;
    @Mock
    private MicroOrganismDAO organismDAO;
    @Mock
    private MicroPatientOriginDAO patientOriginDAO;
    @Mock
    private MicroAntibioticDAO antibioticDAO;
    @Mock
    private MicroWorklistContextDAO worklistContextDAO;
    private MicroWhonetDatasetService service;
    private Map<String, MicroWhonetPatientContext> patientContextsBySampleItem;

    @Before
    public void setUp() {
        patientContextsBySampleItem = new LinkedHashMap<>();
        when(worklistContextDAO.getWhonetPatientContexts(any())).thenAnswer(invocation -> {
            List<String> sampleItemIds = invocation.getArgument(0);
            return sampleItemIds.stream().map(patientContextsBySampleItem::get).filter(value -> value != null).toList();
        });
        service = new MicroWhonetDatasetServiceImpl(caseDAO, caseOrderDetailDAO, isolateDAO, astRunDAO, astReadingDAO,
                organismDAO, patientOriginDAO, antibioticDAO, worklistContextDAO);
        when(caseOrderDetailDAO.getByCaseIds(any())).thenAnswer(invocation -> {
            List<String> caseIds = invocation.getArgument(0);
            return caseIds.stream().map(caseId -> orderDetail(caseId, null, "CLINICAL_DIAGNOSTIC")).toList();
        });
    }

    @Test
    public void previewIncludesEveryMappedReadingFromTheReportableReviewedRun() {
        MicroCase microCase = finalizedCase("case-1", "item-1", "2026-07-12 10:00:00");
        MicroIsolate isolate = isolate("isolate-1", "case-1", "organism-1");
        MicroAstRun run = reviewedRun("run-1", "isolate-1");
        MicroAstReading cip = reading("reading-1", "run-1", "antibiotic-1", "SUSCEPTIBLE");
        MicroAstReading gen = reading("reading-2", "run-1", "antibiotic-2", "RESISTANT");

        when(caseDAO.getFinalizedBacteriologyByCollectionDateRange(any(Timestamp.class), any(Timestamp.class)))
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
    public void previewAndExportUseTheMappedWhonetSpecimenCode() {
        MicroCase microCase = finalizedCase("case-1", "item-1", "2026-07-12 10:00:00");
        MicroIsolate isolate = isolate("isolate-1", "case-1", "organism-1");
        MicroAstRun run = reviewedRun("run-1", "isolate-1");

        when(caseDAO.getFinalizedBacteriologyByCollectionDateRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(microCase));
        when(isolateDAO.getByCaseIds(List.of("case-1"))).thenReturn(List.of(isolate));
        when(astRunDAO.getByIsolateIds(List.of("isolate-1"))).thenReturn(List.of(run));
        when(astReadingDAO.getByRunIds(List.of("run-1")))
                .thenReturn(List.of(reading("reading-1", "run-1", "antibiotic-1", "S")));
        when(organismDAO.get("organism-1")).thenReturn(Optional.of(organism("organism-1", "eco", "E. coli")));
        when(antibioticDAO.get("antibiotic-1"))
                .thenReturn(Optional.of(antibiotic("antibiotic-1", "CIP", "Ciprofloxacin")));
        stubPatientContext("case-1", "item-1", "patient-1", "LAB-001", "sample-type-1", "BLD");

        MicroWhonetDataset dataset = service.compile(query("NONE"));

        assertEquals("BLD", dataset.getPreview().rows.get(0).specimenType);
        assertTrue(dataset.getRows().get(0).getRow().contains("\"BLD\""));
        assertFalse(dataset.getRows().get(0).getRow().contains("\"Blood\""));
    }

    @Test
    public void previewExcludesOnlyRowsForAnUnmappedSpecimenAndProvidesExactRepairTarget() {
        MicroCase mappedCase = finalizedCase("case-1", "item-1", "2026-07-12 10:00:00");
        MicroCase unmappedCase = finalizedCase("case-2", "item-2", "2026-07-13 10:00:00");
        MicroIsolate mappedIsolate = isolate("isolate-1", "case-1", "organism-1");
        MicroIsolate unmappedIsolate = isolate("isolate-2", "case-2", "organism-1");
        MicroAstRun mappedRun = reviewedRun("run-1", "isolate-1");
        MicroAstRun unmappedRun = reviewedRun("run-2", "isolate-2");

        when(caseDAO.getFinalizedBacteriologyByCollectionDateRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(mappedCase, unmappedCase));
        when(isolateDAO.getByCaseIds(List.of("case-1", "case-2"))).thenReturn(List.of(mappedIsolate, unmappedIsolate));
        when(astRunDAO.getByIsolateIds(List.of("isolate-1", "isolate-2"))).thenReturn(List.of(mappedRun, unmappedRun));
        when(astReadingDAO.getByRunIds(List.of("run-1", "run-2"))).thenReturn(List.of(
                reading("reading-1", "run-1", "antibiotic-1", "S"), reading("reading-2", "run-2", "antibiotic-1", "R"),
                reading("reading-3", "run-2", "antibiotic-1", "I")));
        when(organismDAO.get("organism-1")).thenReturn(Optional.of(organism("organism-1", "eco", "E. coli")));
        when(antibioticDAO.get("antibiotic-1"))
                .thenReturn(Optional.of(antibiotic("antibiotic-1", "CIP", "Ciprofloxacin")));
        stubPatientContext("case-1", "item-1", "patient-1", "LAB-001", "sample-type-mapped", "BLD");
        stubPatientContext("case-2", "item-2", "patient-2", "LAB-002", "sample-type-unmapped", "");

        MicroWhonetPreviewForm preview = service.compile(query("NONE")).getPreview();

        assertTrue(preview.canGenerate);
        assertEquals(1, preview.exportedRows);
        assertEquals(2, preview.excludedRows);
        assertEquals(1, preview.warnings.size());
        assertEquals("SPECIMEN_MAPPING_REQUIRED", preview.warnings.get(0).code);
        assertEquals("specimen-types", preview.warnings.get(0).resource);
        assertEquals("sample-type-unmapped", preview.warnings.get(0).resourceId);
        assertEquals("Blood", preview.warnings.get(0).itemLabel);
        assertEquals(2, preview.warnings.get(0).excludedRows);
    }

    @Test
    public void previewExcludesOnlyUnmappedReadingsAndProvidesExactRepairTarget() {
        MicroCase microCase = finalizedCase("case-1", "item-1", "2026-07-12 10:00:00");
        MicroIsolate isolate = isolate("isolate-1", "case-1", "organism-1");
        MicroAstRun run = reviewedRun("run-1", "isolate-1");

        when(caseDAO.getFinalizedBacteriologyByCollectionDateRange(any(Timestamp.class), any(Timestamp.class)))
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

        when(caseDAO.getFinalizedBacteriologyByCollectionDateRange(any(Timestamp.class), any(Timestamp.class)))
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
    public void previewExcludesAnUnidentifiedIsolateWithoutFailingTheReportingPeriod() {
        MicroCase microCase = finalizedCase("case-1", "item-1", "2026-07-12 10:00:00");
        MicroIsolate unidentified = isolate("isolate-1", "case-1", null);

        when(caseDAO.getFinalizedBacteriologyByCollectionDateRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(microCase));
        when(isolateDAO.getByCaseIds(List.of("case-1"))).thenReturn(List.of(unidentified));
        when(astRunDAO.getByIsolateIds(List.of("isolate-1"))).thenReturn(List.of());
        stubPatientContext("case-1", "item-1", "patient-1", "LAB-001");

        MicroWhonetPreviewForm preview = service.compile(query("NONE")).getPreview();

        assertFalse(preview.canGenerate);
        assertEquals(0, preview.exportedRows);
        assertEquals(1, preview.excludedRows);
        assertEquals(1, preview.warnings.size());
        assertEquals("ORGANISM_MAPPING_REQUIRED", preview.warnings.get(0).code);
        assertEquals("ISO-1", preview.warnings.get(0).itemLabel);
        verify(organismDAO, never()).get(anyString());
    }

    @Test
    public void previewExcludesReadingsWithoutAWhonetSirInterpretation() {
        MicroCase microCase = finalizedCase("case-1", "item-1", "2026-07-12 10:00:00");
        MicroIsolate isolate = isolate("isolate-1", "case-1", "organism-1");
        MicroAstRun run = reviewedRun("run-1", "isolate-1");

        when(caseDAO.getFinalizedBacteriologyByCollectionDateRange(any(Timestamp.class), any(Timestamp.class)))
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

        when(caseDAO.getFinalizedBacteriologyByCollectionDateRange(any(Timestamp.class), any(Timestamp.class)))
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
    public void previewAppliesEveryPopulationFilterBeforeDeduplication() {
        MicroCase bloodClinicalCase = finalizedCase("case-1", "item-1", "2026-07-12 10:00:00");
        MicroCase urineFloraCase = finalizedCase("case-2", "item-2", "2026-07-13 10:00:00");
        MicroCase bloodContaminantCase = finalizedCase("case-3", "item-3", "2026-07-14 10:00:00");
        MicroIsolate bloodClinical = isolate("isolate-1", "case-1", "organism-1");
        MicroIsolate urineFlora = isolate("isolate-2", "case-2", "organism-2");
        urineFlora.setSignificance(MicroIsolateSignificance.NORMAL_FLORA.name());
        MicroIsolate bloodContaminant = isolate("isolate-3", "case-3", "organism-2");
        bloodContaminant.setSignificance(MicroIsolateSignificance.CONTAMINANT.name());
        MicroAstRun selectedRun = reviewedRun("run-3", "isolate-3");

        when(caseDAO.getFinalizedBacteriologyByCollectionDateRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(bloodClinicalCase, urineFloraCase, bloodContaminantCase));
        when(caseOrderDetailDAO.getByCaseIds(List.of("case-1", "case-2", "case-3")))
                .thenReturn(List.of(orderDetail("case-1", "OUTPATIENT"), orderDetail("case-2", "INPATIENT"),
                        orderDetail("case-3", "INPATIENT")));
        when(isolateDAO.getByCaseIds(List.of("case-1", "case-2", "case-3")))
                .thenReturn(List.of(bloodClinical, urineFlora, bloodContaminant));
        when(astRunDAO.getByIsolateIds(List.of("isolate-3"))).thenReturn(List.of(selectedRun));
        when(astReadingDAO.getByRunIds(List.of("run-3")))
                .thenReturn(List.of(reading("reading-3", "run-3", "antibiotic-1", "R")));
        when(organismDAO.get("organism-2")).thenReturn(Optional.of(organism("organism-2", "sau", "S. aureus")));
        when(antibioticDAO.get("antibiotic-1"))
                .thenReturn(Optional.of(antibiotic("antibiotic-1", "CIP", "Ciprofloxacin")));
        stubPatientContext("case-1", "item-1", "patient-1", "LAB-001", "sample-type-blood", "BLD");
        stubPatientContext("case-2", "item-2", "patient-2", "LAB-002", "sample-type-urine", "URI");
        stubPatientContext("case-3", "item-3", "patient-3", "LAB-003", "sample-type-blood", "BLD");

        MicroWhonetExportQueryForm query = query("NONE");
        query.specimen = List.of("sample-type-blood");
        query.organism = List.of("organism-2");
        query.origin = List.of("INPATIENT");
        query.significance = List.of(MicroIsolateSignificance.CONTAMINANT.name());
        MicroWhonetDataset dataset = service.compile(query);
        MicroWhonetPreviewForm preview = dataset.getPreview();

        assertEquals(2, preview.afterSpecimen);
        assertEquals(1, preview.afterOrganism);
        assertEquals(1, preview.afterPatientOrigin);
        assertEquals(1, preview.afterSignificance);
        assertEquals(1, preview.afterDeduplication);
        assertEquals(1, preview.exportedRows);
        assertEquals("case-3", preview.rows.get(0).caseId);
        assertEquals(List.of("sample-type-blood"), dataset.getPopulationSelection().getSpecimen());
        assertEquals(List.of("organism-2"), dataset.getPopulationSelection().getOrganism());
        assertEquals(List.of("INPATIENT"), dataset.getPopulationSelection().getOrigin());
        assertEquals(List.of(MicroIsolateSignificance.CONTAMINANT.name()),
                dataset.getPopulationSelection().getSignificance());
    }

    @Test
    public void previewExcludesScreeningAndUnspecifiedPurposesUntilExplicitlyIncluded() {
        MicroCase clinicalCase = finalizedCase("case-clinical", "item-clinical", "2026-07-12 10:00:00");
        MicroCase screeningCase = finalizedCase("case-screening", "item-screening", "2026-07-13 10:00:00");
        MicroCase unspecifiedCase = finalizedCase("case-unspecified", "item-unspecified", "2026-07-14 10:00:00");
        List<MicroCase> cases = List.of(clinicalCase, screeningCase, unspecifiedCase);
        List<MicroIsolate> isolates = List.of(isolate("isolate-clinical", "case-clinical", "organism-1"),
                isolate("isolate-screening", "case-screening", "organism-1"),
                isolate("isolate-unspecified", "case-unspecified", "organism-1"));

        when(caseDAO.getFinalizedBacteriologyByCollectionDateRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(cases);
        when(caseOrderDetailDAO.getByCaseIds(List.of("case-clinical", "case-screening", "case-unspecified")))
                .thenReturn(List.of(orderDetail("case-clinical", "INPATIENT", "CLINICAL_DIAGNOSTIC"),
                        orderDetail("case-screening", "INPATIENT", "ACTIVE_SCREENING"),
                        orderDetail("case-unspecified", "INPATIENT", null)));
        when(isolateDAO.getByCaseIds(List.of("case-clinical", "case-screening", "case-unspecified")))
                .thenReturn(isolates);
        when(astRunDAO.getByIsolateIds(List.of("isolate-clinical")))
                .thenReturn(List.of(reviewedRun("run-clinical", "isolate-clinical")));
        when(astReadingDAO.getByRunIds(List.of("run-clinical")))
                .thenReturn(List.of(reading("reading-clinical", "run-clinical", "antibiotic-1", "S")));
        when(organismDAO.get("organism-1")).thenReturn(Optional.of(organism("organism-1", "eco", "E. coli")));
        when(antibioticDAO.get("antibiotic-1"))
                .thenReturn(Optional.of(antibiotic("antibiotic-1", "CIP", "Ciprofloxacin")));
        stubPatientContext("case-clinical", "item-clinical", "patient-clinical", "LAB-CLINICAL");
        stubPatientContext("case-screening", "item-screening", "patient-screening", "LAB-SCREENING");
        stubPatientContext("case-unspecified", "item-unspecified", "patient-unspecified", "LAB-UNSPECIFIED");

        MicroWhonetDataset dataset = service.compile(query("NONE"));

        assertEquals(1, dataset.getPreview().clinicalPurposeCases);
        assertEquals(1, dataset.getPreview().screeningPurposeCases);
        assertEquals(1, dataset.getPreview().unspecifiedPurposeCases);
        assertEquals(1, dataset.getPreview().afterCulturePurpose);
        assertEquals(1, dataset.getPreview().exportedRows);
        assertEquals("case-clinical", dataset.getPreview().rows.get(0).caseId);
        assertFalse(dataset.getPopulationSelection().isIncludeScreening());
        assertFalse(dataset.getPopulationSelection().isIncludeUnspecified());
    }

    @Test
    public void previewIncludesScreeningAndUnspecifiedPurposesOnlyWhenEachFlagIsTrue() {
        MicroCase screeningCase = finalizedCase("case-screening", "item-screening", "2026-07-13 10:00:00");
        MicroCase unspecifiedCase = finalizedCase("case-unspecified", "item-unspecified", "2026-07-14 10:00:00");
        when(caseDAO.getFinalizedBacteriologyByCollectionDateRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(screeningCase, unspecifiedCase));
        when(caseOrderDetailDAO.getByCaseIds(List.of("case-screening", "case-unspecified")))
                .thenReturn(List.of(orderDetail("case-screening", "INPATIENT", "ACTIVE_SCREENING"),
                        orderDetail("case-unspecified", "INPATIENT", null)));
        MicroIsolate screening = isolate("isolate-screening", "case-screening", "organism-1");
        MicroIsolate unspecified = isolate("isolate-unspecified", "case-unspecified", "organism-1");
        when(isolateDAO.getByCaseIds(List.of("case-screening", "case-unspecified")))
                .thenReturn(List.of(screening, unspecified));
        when(astRunDAO.getByIsolateIds(List.of("isolate-screening", "isolate-unspecified")))
                .thenReturn(List.of(reviewedRun("run-screening", "isolate-screening"),
                        reviewedRun("run-unspecified", "isolate-unspecified")));
        when(astReadingDAO.getByRunIds(List.of("run-screening", "run-unspecified")))
                .thenReturn(List.of(reading("reading-screening", "run-screening", "antibiotic-1", "S"),
                        reading("reading-unspecified", "run-unspecified", "antibiotic-1", "R")));
        when(organismDAO.get("organism-1")).thenReturn(Optional.of(organism("organism-1", "eco", "E. coli")));
        when(antibioticDAO.get("antibiotic-1"))
                .thenReturn(Optional.of(antibiotic("antibiotic-1", "CIP", "Ciprofloxacin")));
        stubPatientContext("case-screening", "item-screening", "patient-screening", "LAB-SCREENING");
        stubPatientContext("case-unspecified", "item-unspecified", "patient-unspecified", "LAB-UNSPECIFIED");
        MicroWhonetExportQueryForm query = query("NONE");
        query.includeScreening = true;
        query.includeUnspecified = true;

        MicroWhonetDataset dataset = service.compile(query);

        assertEquals(2, dataset.getPreview().afterCulturePurpose);
        assertEquals(2, dataset.getPreview().exportedRows);
        assertTrue(dataset.getPopulationSelection().isIncludeScreening());
        assertTrue(dataset.getPopulationSelection().isIncludeUnspecified());
    }

    @Test
    public void filterOptionsContainOnlyValuesPresentInTheReportingPeriod() {
        MicroCase bloodCase = finalizedCase("case-1", "item-1", "2026-07-12 10:00:00");
        MicroCase urineCase = finalizedCase("case-2", "item-2", "2026-07-13 10:00:00");
        MicroIsolate clinical = isolate("isolate-1", "case-1", "organism-1");
        MicroIsolate flora = isolate("isolate-2", "case-2", "organism-2");
        flora.setSignificance(MicroIsolateSignificance.NORMAL_FLORA.name());

        when(caseDAO.getFinalizedBacteriologyByCollectionDateRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(bloodCase, urineCase));
        when(caseOrderDetailDAO.getByCaseIds(List.of("case-1", "case-2")))
                .thenReturn(List.of(orderDetail("case-1", "OUTPATIENT"), orderDetail("case-2", "INPATIENT")));
        when(isolateDAO.getByCaseIds(List.of("case-1", "case-2"))).thenReturn(List.of(clinical, flora));
        when(organismDAO.getByIds(List.of("organism-1", "organism-2"))).thenReturn(
                List.of(organism("organism-1", "eco", "E. coli"), organism("organism-2", "sau", "S. aureus")));
        when(patientOriginDAO.getByCodes(List.of("INPATIENT", "OUTPATIENT"))).thenReturn(
                List.of(patientOrigin("INPATIENT", "Inpatient"), patientOrigin("OUTPATIENT", "Outpatient")));
        stubPatientContext("case-1", "item-1", "patient-1", "LAB-001", "sample-type-blood", "BLD");
        stubPatientContext("case-2", "item-2", "patient-2", "LAB-002", "sample-type-urine", "URI");

        MicroWhonetFilterOptionsForm options = service.getFilterOptions(query("NONE"));

        assertEquals(List.of("sample-type-blood", "sample-type-urine"),
                options.specimenTypes.stream().map(option -> option.id).toList());
        assertEquals(List.of("organism-1", "organism-2"), options.organisms.stream().map(option -> option.id).toList());
        assertEquals(List.of("INPATIENT", "OUTPATIENT"),
                options.patientOrigins.stream().map(option -> option.id).toList());
        assertEquals(List.of("CLINICALLY_SIGNIFICANT", "NORMAL_FLORA"),
                options.significance.stream().map(option -> option.id).toList());
        verify(worklistContextDAO).getWhonetPatientContexts(List.of("item-1", "item-2"));
    }

    @Test
    public void filterOptionsAreEmptyWhenTheReportingPeriodHasNoCases() {
        when(caseDAO.getFinalizedBacteriologyByCollectionDateRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());

        MicroWhonetFilterOptionsForm options = service.getFilterOptions(query("NONE"));

        assertTrue(options.specimenTypes.isEmpty());
        assertTrue(options.organisms.isEmpty());
        assertTrue(options.patientOrigins.isEmpty());
        assertTrue(options.significance.isEmpty());
        verify(worklistContextDAO, never()).getWhonetPatientContexts(any());
        verify(caseOrderDetailDAO, never()).getByCaseIds(any());
        verify(isolateDAO, never()).getByCaseIds(any());
    }

    @Test
    public void filterOptionsOmitUnidentifiedIsolatesWithoutFailingTheReportingPeriod() {
        MicroCase microCase = finalizedCase("case-1", "item-1", "2026-07-12 10:00:00");
        MicroIsolate unidentified = isolate("isolate-1", "case-1", null);

        when(caseDAO.getFinalizedBacteriologyByCollectionDateRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(microCase));
        when(caseOrderDetailDAO.getByCaseIds(List.of("case-1"))).thenReturn(List.of());
        when(isolateDAO.getByCaseIds(List.of("case-1"))).thenReturn(List.of(unidentified));
        stubPatientContext("case-1", "item-1", "patient-1", "LAB-001");

        MicroWhonetFilterOptionsForm options = service.getFilterOptions(query("NONE"));

        assertTrue(options.organisms.isEmpty());
        assertEquals(List.of(MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name()),
                options.significance.stream().map(option -> option.id).toList());
        verify(organismDAO, never()).getByIds(any());
    }

    @Test
    public void filterOptionsUseTheOriginCodeWhenItsDisplayNameIsMissing() {
        MicroCase microCase = finalizedCase("case-1", "item-1", "2026-07-12 10:00:00");
        MicroIsolate isolate = isolate("isolate-1", "case-1", "organism-1");

        when(caseDAO.getFinalizedBacteriologyByCollectionDateRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(microCase));
        when(caseOrderDetailDAO.getByCaseIds(List.of("case-1")))
                .thenReturn(List.of(orderDetail("case-1", "LEGACY_ORIGIN")));
        when(isolateDAO.getByCaseIds(List.of("case-1"))).thenReturn(List.of(isolate));
        when(organismDAO.getByIds(List.of("organism-1"))).thenReturn(List.of(organism("organism-1", "eco", "E. coli")));
        when(patientOriginDAO.getByCodes(List.of("LEGACY_ORIGIN")))
                .thenReturn(List.of(patientOrigin("LEGACY_ORIGIN", null)));
        stubPatientContext("case-1", "item-1", "patient-1", "LAB-001");

        MicroWhonetFilterOptionsForm options = service.getFilterOptions(query("NONE"));

        assertEquals(1, options.patientOrigins.size());
        assertEquals("LEGACY_ORIGIN", options.patientOrigins.get(0).id);
        assertEquals("LEGACY_ORIGIN", options.patientOrigins.get(0).label);
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

        when(caseDAO.getFinalizedBacteriologyByCollectionDateRange(any(Timestamp.class), any(Timestamp.class)))
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
        MicroCase firstCase = finalizedCase("case-1", "item-1", "2026-07-20 10:00:00");
        MicroCase repeatCase = finalizedCase("case-2", "item-2", "2026-07-15 10:00:00");
        MicroIsolate first = isolate("isolate-1", "case-1", "organism-1");
        MicroIsolate repeat = isolate("isolate-2", "case-2", "organism-1");
        MicroAstRun firstRun = reviewedRun("run-1", "isolate-1");

        when(caseDAO.getFinalizedBacteriologyByCollectionDateRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(repeatCase, firstCase));
        when(isolateDAO.getByCaseIds(List.of("case-2", "case-1"))).thenReturn(List.of(repeat, first));
        when(astRunDAO.getByIsolateIds(List.of("isolate-1"))).thenReturn(List.of(firstRun));
        when(astReadingDAO.getByRunIds(List.of("run-1")))
                .thenReturn(List.of(reading("reading-1", "run-1", "antibiotic-1", "S")));
        when(organismDAO.get("organism-1")).thenReturn(Optional.of(organism("organism-1", "eco", "E. coli")));
        when(antibioticDAO.get("antibiotic-1"))
                .thenReturn(Optional.of(antibiotic("antibiotic-1", "CIP", "Ciprofloxacin")));
        stubPatientContext("case-1", "item-1", "patient-1", "LAB-001", "2026-07-10 10:00:00");
        stubPatientContext("case-2", "item-2", "patient-1", "LAB-002", "2026-07-14 10:00:00");

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
        when(caseDAO.getFinalizedBacteriologyByCollectionDateRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(firstCase, secondCase));
        when(isolateDAO.getByCaseIds(List.of("case-1", "case-2"))).thenReturn(List.of());

        service.compile(query("NONE"));

        verify(worklistContextDAO).getWhonetPatientContexts(List.of("item-1", "item-2"));
    }

    @Test
    public void sevenDayBoundaryIncludesAnIsolateAtExactlySevenDays() {
        MicroCase firstCase = finalizedCase("case-1", "item-1", "2026-07-20 10:00:00");
        MicroCase insideWindow = finalizedCase("case-2", "item-2", "2026-07-11 09:59:59");
        MicroCase boundaryCase = finalizedCase("case-3", "item-3", "2026-07-12 10:00:00");
        MicroIsolate first = isolate("isolate-1", "case-1", "organism-1");
        MicroIsolate inside = isolate("isolate-2", "case-2", "organism-1");
        MicroIsolate boundary = isolate("isolate-3", "case-3", "organism-1");
        MicroAstRun firstRun = reviewedRun("run-1", "isolate-1");
        MicroAstRun boundaryRun = reviewedRun("run-3", "isolate-3");

        when(caseDAO.getFinalizedBacteriologyByCollectionDateRange(any(Timestamp.class), any(Timestamp.class)))
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
        stubPatientContext("case-1", "item-1", "patient-1", "LAB-001", "2026-07-10 10:00:00");
        stubPatientContext("case-2", "item-2", "patient-1", "LAB-002", "2026-07-16 23:59:59");
        stubPatientContext("case-3", "item-3", "patient-1", "LAB-003", "2026-07-17 10:00:00");

        MicroWhonetPreviewForm preview = service.compile(query("FIRST_ISOLATE_7_DAY")).getPreview();

        assertEquals(3, preview.afterSignificance);
        assertEquals(2, preview.afterDeduplication);
        assertEquals(2, preview.exportedRows);
        assertTrue(preview.rows.stream().anyMatch(row -> "case-1".equals(row.caseId)));
        assertFalse(preview.rows.stream().anyMatch(row -> "case-2".equals(row.caseId)));
        assertTrue(preview.rows.stream().anyMatch(row -> "case-3".equals(row.caseId)));
    }

    @Test
    public void sevenDayBoundaryUsesCalendarDaysAcrossDaylightSavingTransition() {
        MicroCase firstCase = finalizedCase("case-1", "item-1", "2026-03-01 18:00:00");
        MicroCase boundaryCase = finalizedCase("case-2", "item-2", "2026-03-08 17:00:00");
        MicroIsolate first = isolate("isolate-1", "case-1", "organism-1");
        MicroIsolate boundary = isolate("isolate-2", "case-2", "organism-1");
        MicroAstRun firstRun = reviewedRun("run-1", "isolate-1");
        MicroAstRun boundaryRun = reviewedRun("run-2", "isolate-2");
        stubDataset(List.of(firstCase, boundaryCase), List.of(first, boundary), List.of(firstRun, boundaryRun),
                List.of(reading("reading-1", "run-1", "antibiotic-1", "S"),
                        reading("reading-2", "run-2", "antibiotic-1", "S")));
        stubMappedReferences();
        ZoneId laboratoryZone = ZoneId.of("America/Los_Angeles");
        stubPatientContext("case-1", "item-1", "patient-1", "LAB-001",
                Timestamp.from(ZonedDateTime.of(2026, 3, 1, 10, 0, 0, 0, laboratoryZone).toInstant()));
        stubPatientContext("case-2", "item-2", "patient-1", "LAB-002",
                Timestamp.from(ZonedDateTime.of(2026, 3, 8, 10, 0, 0, 0, laboratoryZone).toInstant()));
        MicroWhonetExportQueryForm query = query("FIRST_ISOLATE_7_DAY");
        query.from = "2026-03-01";
        query.to = "2026-03-31";

        MicroWhonetPreviewForm preview = service.compile(query).getPreview();

        assertEquals(2, preview.afterDeduplication);
        assertTrue(preview.rows.stream().anyMatch(row -> "case-1".equals(row.caseId)));
        assertTrue(preview.rows.stream().anyMatch(row -> "case-2".equals(row.caseId)));
    }

    @Test
    public void configurableWindowIncludesAnIsolateAtFourteenDaysButCollapsesItAtThirtyDays() {
        MicroCase firstCase = finalizedCase("case-1", "item-1", "2026-07-20 10:00:00");
        MicroCase boundaryCase = finalizedCase("case-2", "item-2", "2026-07-21 10:00:00");
        MicroIsolate first = isolate("isolate-1", "case-1", "organism-1");
        MicroIsolate boundary = isolate("isolate-2", "case-2", "organism-1");
        MicroAstRun firstRun = reviewedRun("run-1", "isolate-1");
        MicroAstRun boundaryRun = reviewedRun("run-2", "isolate-2");
        stubDataset(List.of(firstCase, boundaryCase), List.of(first, boundary), List.of(firstRun, boundaryRun),
                List.of(reading("reading-1", "run-1", "antibiotic-1", "S"),
                        reading("reading-2", "run-2", "antibiotic-1", "S")));
        stubMappedReferences();
        stubPatientContext("case-1", "item-1", "patient-1", "LAB-001", "2026-07-01 10:00:00");
        stubPatientContext("case-2", "item-2", "patient-1", "LAB-002", "2026-07-15 10:00:00");

        MicroWhonetPreviewForm fourteenDay = service.compile(query("FIRST_ISOLATE_14_DAY")).getPreview();
        MicroWhonetPreviewForm thirtyDay = service.compile(query("FIRST_ISOLATE_30_DAY")).getPreview();

        assertEquals(2, fourteenDay.afterDeduplication);
        assertEquals(1, thirtyDay.afterDeduplication);
    }

    @Test
    public void releaseDateBasisChangesOnlyDeduplicationChronology() {
        MicroCase firstCase = finalizedCase("case-1", "item-1", "2026-07-20 10:00:00");
        MicroCase secondCase = finalizedCase("case-2", "item-2", "2026-07-10 10:00:00");
        MicroIsolate first = isolate("isolate-1", "case-1", "organism-1");
        MicroIsolate second = isolate("isolate-2", "case-2", "organism-1");
        MicroAstRun firstRun = reviewedRun("run-1", "isolate-1");
        MicroAstRun secondRun = reviewedRun("run-2", "isolate-2");
        stubDataset(List.of(firstCase, secondCase), List.of(first, second), List.of(firstRun, secondRun),
                List.of(reading("reading-1", "run-1", "antibiotic-1", "S"),
                        reading("reading-2", "run-2", "antibiotic-1", "R")));
        stubMappedReferences();
        stubPatientContext("case-1", "item-1", "patient-1", "LAB-001", "2026-07-01 10:00:00");
        stubPatientContext("case-2", "item-2", "patient-1", "LAB-002", "2026-07-04 10:00:00");

        MicroWhonetExportQueryForm collectionQuery = query("FIRST_ISOLATE_7_DAY");
        MicroWhonetExportQueryForm releaseQuery = query("FIRST_ISOLATE_7_DAY");
        releaseQuery.dedupBasis = "RELEASE_DATE";

        assertEquals(1, service.compile(collectionQuery).getPreview().afterDeduplication);
        assertEquals(2, service.compile(releaseQuery).getPreview().afterDeduplication);
        verify(caseDAO, times(2)).getFinalizedBacteriologyByCollectionDateRange(
                Timestamp.valueOf("2026-07-01 00:00:00"), Timestamp.valueOf("2026-08-01 00:00:00"));
    }

    @Test
    public void sameSourceScopeRetainsDifferentSpecimenSourcesWithinTheWindow() {
        MicroCase bloodCase = finalizedCase("case-1", "item-1", "2026-07-20 10:00:00");
        MicroCase urineCase = finalizedCase("case-2", "item-2", "2026-07-21 10:00:00");
        MicroIsolate blood = isolate("isolate-1", "case-1", "organism-1");
        MicroIsolate urine = isolate("isolate-2", "case-2", "organism-1");
        MicroAstRun bloodRun = reviewedRun("run-1", "isolate-1");
        MicroAstRun urineRun = reviewedRun("run-2", "isolate-2");
        stubDataset(List.of(bloodCase, urineCase), List.of(blood, urine), List.of(bloodRun, urineRun),
                List.of(reading("reading-1", "run-1", "antibiotic-1", "S"),
                        reading("reading-2", "run-2", "antibiotic-1", "S")));
        stubMappedReferences();
        stubPatientContext("case-1", "item-1", "patient-1", "LAB-001", "sample-type-blood", "BLD",
                "2026-07-01 10:00:00");
        stubPatientContext("case-2", "item-2", "patient-1", "LAB-002", "sample-type-urine", "URN",
                "2026-07-02 10:00:00");

        MicroWhonetExportQueryForm anySource = query("FIRST_ISOLATE_7_DAY");
        MicroWhonetExportQueryForm sameSource = query("FIRST_ISOLATE_7_DAY");
        sameSource.dedupScope = "SAME_SOURCE";

        assertEquals(1, service.compile(anySource).getPreview().afterDeduplication);
        assertEquals(2, service.compile(sameSource).getPreview().afterDeduplication);
    }

    @Test
    public void contaminantFirstHandlingRemovesAProbableContaminantBeforeSelection() {
        MicroCase contaminantCase = finalizedCase("case-1", "item-1", "2026-07-20 10:00:00");
        MicroCase clinicalCase = finalizedCase("case-2", "item-2", "2026-07-21 10:00:00");
        MicroIsolate contaminant = isolate("isolate-1", "case-1", "organism-1");
        contaminant.setSignificance(MicroIsolateSignificance.CONTAMINANT.name());
        MicroIsolate clinical = isolate("isolate-2", "case-2", "organism-1");
        MicroAstRun contaminantRun = reviewedRun("run-1", "isolate-1");
        MicroAstRun clinicalRun = reviewedRun("run-2", "isolate-2");
        stubDataset(List.of(contaminantCase, clinicalCase), List.of(contaminant, clinical),
                List.of(contaminantRun, clinicalRun), List.of(reading("reading-1", "run-1", "antibiotic-1", "S"),
                        reading("reading-2", "run-2", "antibiotic-1", "R")));
        stubMappedReferences();
        stubPatientContext("case-1", "item-1", "patient-1", "LAB-001", "2026-07-01 10:00:00");
        stubPatientContext("case-2", "item-2", "patient-1", "LAB-002", "2026-07-02 10:00:00");
        MicroWhonetExportQueryForm includeContaminant = query("FIRST_ISOLATE_7_DAY");
        includeContaminant.significance = List.of(MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name(),
                MicroIsolateSignificance.CONTAMINANT.name());

        MicroWhonetPreviewForm preview = service.compile(includeContaminant).getPreview();

        assertEquals(2, preview.afterSignificance);
        assertEquals(1, preview.afterDeduplication);
        assertEquals("case-2", preview.rows.get(0).caseId);
    }

    @Test
    public void profileSensitiveSelectionRetainsAChangedReviewedSirProfile() {
        MicroCase firstCase = finalizedCase("case-1", "item-1", "2026-07-20 10:00:00");
        MicroCase changedCase = finalizedCase("case-2", "item-2", "2026-07-21 10:00:00");
        MicroIsolate first = isolate("isolate-1", "case-1", "organism-1");
        MicroIsolate changed = isolate("isolate-2", "case-2", "organism-1");
        MicroAstRun firstRun = reviewedRun("run-1", "isolate-1");
        MicroAstRun changedRun = reviewedRun("run-2", "isolate-2");
        stubDataset(List.of(firstCase, changedCase), List.of(first, changed), List.of(firstRun, changedRun),
                List.of(reading("reading-1", "run-1", "antibiotic-1", "S"),
                        reading("reading-2", "run-2", "antibiotic-1", "R")));
        stubMappedReferences();
        stubPatientContext("case-1", "item-1", "patient-1", "LAB-001", "2026-07-01 10:00:00");
        stubPatientContext("case-2", "item-2", "patient-1", "LAB-002", "2026-07-02 10:00:00");

        MicroWhonetExportQueryForm insensitive = query("FIRST_ISOLATE_7_DAY");
        MicroWhonetExportQueryForm sensitive = query("FIRST_ISOLATE_7_DAY");
        sensitive.profileSensitivity = "SENSITIVE";

        assertEquals(1, service.compile(insensitive).getPreview().afterDeduplication);
        MicroWhonetDataset sensitiveDataset = service.compile(sensitive);
        assertEquals(2, sensitiveDataset.getPreview().afterDeduplication);
        assertEquals("SENSITIVE", sensitiveDataset.getPopulationSelection().getProfileSensitivity());
    }

    @Test
    public void collectionDateRangeUsesInclusiveStartAndExclusiveDayAfterEnd() {
        when(caseDAO.getFinalizedBacteriologyByCollectionDateRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());

        MicroWhonetPreviewForm preview = service.compile(query("NONE")).getPreview();

        verify(caseDAO).getFinalizedBacteriologyByCollectionDateRange(Timestamp.valueOf("2026-07-01 00:00:00"),
                Timestamp.valueOf("2026-08-01 00:00:00"));
        assertEquals("2026-07-01", preview.from);
        assertEquals("2026-07-31", preview.to);
        assertFalse(preview.canGenerate);
    }

    private MicroWhonetExportQueryForm query(String dedup) {
        MicroWhonetExportQueryForm query = new MicroWhonetExportQueryForm();
        query.from = LocalDate.of(2026, 7, 1).toString();
        query.to = LocalDate.of(2026, 7, 31).toString();
        query.significance = List.of(MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name());
        query.dedup = dedup;
        query.page = 1;
        query.pageSize = 20;
        return query;
    }

    private void stubDataset(List<MicroCase> cases, List<MicroIsolate> isolates, List<MicroAstRun> runs,
            List<MicroAstReading> readings) {
        when(caseDAO.getFinalizedBacteriologyByCollectionDateRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(cases);
        when(isolateDAO.getByCaseIds(cases.stream().map(MicroCase::getId).toList())).thenReturn(isolates);
        when(astRunDAO.getByIsolateIds(any())).thenAnswer(invocation -> {
            List<String> isolateIds = invocation.getArgument(0);
            return runs.stream().filter(run -> isolateIds.contains(run.getIsolateId())).toList();
        });
        when(astReadingDAO.getByRunIds(any())).thenAnswer(invocation -> {
            List<String> runIds = invocation.getArgument(0);
            return readings.stream().filter(reading -> runIds.contains(reading.getAstRunId())).toList();
        });
    }

    private void stubMappedReferences() {
        when(organismDAO.get("organism-1")).thenReturn(Optional.of(organism("organism-1", "eco", "E. coli")));
        when(antibioticDAO.get("antibiotic-1"))
                .thenReturn(Optional.of(antibiotic("antibiotic-1", "CIP", "Ciprofloxacin")));
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

    private MicroCaseOrderDetail orderDetail(String caseId, String patientOrigin) {
        return orderDetail(caseId, patientOrigin, "CLINICAL_DIAGNOSTIC");
    }

    private MicroCaseOrderDetail orderDetail(String caseId, String patientOrigin, String culturePurpose) {
        MicroCaseOrderDetail detail = new MicroCaseOrderDetail();
        detail.setCaseId(caseId);
        detail.setPatientOrigin(patientOrigin);
        detail.setCulturePurpose(culturePurpose);
        return detail;
    }

    private MicroPatientOrigin patientOrigin(String code, String displayName) {
        MicroPatientOrigin origin = new MicroPatientOrigin();
        origin.setCode(code);
        origin.setDisplayName(displayName);
        return origin;
    }

    private void stubPatientContext(String caseId, String sampleItemId, String patientId, String accession) {
        stubPatientContext(caseId, sampleItemId, patientId, accession, "sample-type-1", "BLD");
    }

    private void stubPatientContext(String caseId, String sampleItemId, String patientId, String accession,
            String collectionDate) {
        stubPatientContext(caseId, sampleItemId, patientId, accession, "sample-type-1", "BLD", collectionDate);
    }

    private void stubPatientContext(String caseId, String sampleItemId, String patientId, String accession,
            String sampleTypeId, String whonetCode) {
        stubPatientContext(caseId, sampleItemId, patientId, accession, sampleTypeId, whonetCode, "2026-07-09 09:00:00");
    }

    private void stubPatientContext(String caseId, String sampleItemId, String patientId, String accession,
            String sampleTypeId, String whonetCode, String collectionDate) {
        stubPatientContext(caseId, sampleItemId, patientId, accession, sampleTypeId, whonetCode,
                Timestamp.valueOf(collectionDate));
    }

    private void stubPatientContext(String caseId, String sampleItemId, String patientId, String accession,
            Timestamp collectionTimestamp) {
        stubPatientContext(caseId, sampleItemId, patientId, accession, "sample-type-1", "BLD", collectionTimestamp);
    }

    private void stubPatientContext(String caseId, String sampleItemId, String patientId, String accession,
            String sampleTypeId, String whonetCode, Timestamp collectionTimestamp) {
        patientContextsBySampleItem.put(sampleItemId,
                new MicroWhonetPatientContext(sampleItemId, patientId, "NAT-001", "Ada", "Lovelace", "F", null,
                        accession, null, collectionTimestamp, sampleTypeId, "Blood", whonetCode, null, null));
    }
}
