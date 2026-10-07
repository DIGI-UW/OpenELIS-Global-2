package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroWhonetExportRunDAO;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.form.MicroWhonetExportQueryForm;
import org.openelisglobal.microbiology.form.MicroWhonetPreviewForm;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioForm;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioRequestForm;
import org.openelisglobal.microbiology.service.MicroAstService;
import org.openelisglobal.microbiology.service.MicroIsolateService;
import org.openelisglobal.microbiology.service.MicroReportReleaseService;
import org.openelisglobal.microbiology.service.MicroWhonetDatasetService;
import org.openelisglobal.microbiology.service.MicroWhonetExportBlockedException;
import org.openelisglobal.microbiology.service.MicrobiologyUatScenarioService;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroExportReportingTrack;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.openelisglobal.microbiology.valueholder.MicroWhonetExportRun;
import org.openelisglobal.reports.service.MicroWhonetExportResult;
import org.openelisglobal.reports.service.WHONetReportServiceImpl;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MicroWhonetPersistenceIntegrationTest extends BaseWebContextSensitiveTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private MicrobiologyTestFixtures fixtures;

    @Autowired
    private MicrobiologyUatScenarioService uatScenarioService;

    @Autowired
    private MicroIsolateService isolateService;

    @Autowired
    private MicroAstService astService;

    @Autowired
    private MicroReportReleaseService reportReleaseService;

    @Autowired
    private MicroWhonetDatasetService datasetService;

    @Autowired
    private MicroCaseDAO caseDAO;

    @Autowired
    private MicroWhonetExportRunDAO exportRunDAO;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        fixtures.ensureRequiredWorkflowStatuses();
    }

    @Test
    public void missingReportingTracksBlockPreviewAndGenerationWithoutAudit() {
        MicroWhonetExportQueryForm query = new MicroWhonetExportQueryForm();
        query.from = "2026-07-01";
        query.to = "2026-07-31";
        WHONetReportServiceImpl reportService = new WHONetReportServiceImpl(datasetService, exportRunDAO);
        long before = exportRunDAO.getAll().size();
        org.junit.Assert.assertThrows(MicroWhonetExportBlockedException.class,
                () -> reportService.previewMicrobiologyExport(query));
        org.junit.Assert.assertThrows(MicroWhonetExportBlockedException.class,
                () -> reportService.generateMicrobiologyExport(query, fixtures.defaultUserId()));
        assertEquals(before, exportRunDAO.getAll().size());
    }

    @Test
    public void configuredReportingTracksAreScopedToTheRequestedExport() {
        assertFalse(caseDAO.hasExportReportingTracks("WHONET"));
        MicroExportReportingTrack track = new MicroExportReportingTrack();
        track.setExportKey("WHONET");
        track.setReportingTrackId(entityManager.createQuery("select d.id from Dictionary d order by d.id", String.class)
                .setMaxResults(1).getSingleResult());
        entityManager.persist(track);
        entityManager.flush();
        assertTrue(caseDAO.hasExportReportingTracks("WHONET"));
        assertFalse(caseDAO.hasExportReportingTracks("OTHER"));
    }

    @Test
    public void serviceCreatedFinalCaseIsSelectedAndExportAuditRoundTrips() throws Exception {
        String performedBy = fixtures.defaultUserId();
        MicrobiologyUatScenarioRequestForm request = new MicrobiologyUatScenarioRequestForm();
        request.scenario = "M4";
        request.scenarioKey = "integration-m4-" + UUID.randomUUID();
        MicrobiologyUatScenarioForm scenario = uatScenarioService.provision(request, performedBy);

        Timestamp collectionDate = Timestamp.valueOf("2026-07-12 09:00:00");
        SampleItem sampleItem = sampleItemService.get(scenario.sampleItemId);
        sampleItem.setCollectionDate(collectionDate);
        sampleItem.setSysUserId(performedBy);
        sampleItemService.update(sampleItem);

        TypeOfSample sampleType = typeOfSampleService.get(scenario.sampleTypeId);
        sampleType.setWhonetCode("BLD");
        sampleType.setSysUserId(performedBy);
        typeOfSampleService.update(sampleType);

        MicroIsolate isolate = isolateService.createIsolate(scenario.caseId, scenario.sampleItemId,
                "WHONET-INTEGRATION", "Gram negative rods", "Lactose fermenting colonies",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, performedBy);
        isolateService.updateIdentification(isolate.getId(), scenario.organismId, "Reference organism (integration)",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicroIsolateIdentificationStatus.CONFIRMED,
                "MALDI_TOF", new BigDecimal("99.5"), performedBy);
        MicroAstRun run = astService.startRun(isolate.getId(), scenario.astPanelId, scenario.activeBreakpointStandardId,
                performedBy);
        astService.getPanelAntibiotics(scenario.astPanelId).forEach(ordered -> astService.recordReading(run.getId(),
                ordered.getAntibioticId(), MicroAstMethod.MIC, new BigDecimal("4"), performedBy));
        astService.reviewRun(run.getId(), performedBy);
        MicroCase released = reportReleaseService.releaseFinal(scenario.caseId, performedBy);

        assertTrue(released.getClosedAt().after(new Timestamp(collectionDate.getTime() + 1_000)));
        assertEquals(List.of(released), caseDAO.getFinalizedForExportByCollectionDateRange("WHONET", collectionDate,
                new Timestamp(collectionDate.getTime() + 1_000)));
        assertFalse(caseDAO.getFinalizedForExportByCollectionDateRange("WHONET",
                new Timestamp(collectionDate.getTime() - 1_000), collectionDate).contains(released));

        LocalDate exportDate = collectionDate.toLocalDateTime().toLocalDate();
        MicroWhonetExportQueryForm exportQuery = new MicroWhonetExportQueryForm();
        exportQuery.from = exportDate.toString();
        exportQuery.to = exportDate.toString();
        exportQuery.specimen = List.of(scenario.sampleTypeId);
        exportQuery.organism = List.of(scenario.organismId);
        exportQuery.significance = List.of(MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name());
        exportQuery.includeUnspecified = true;
        exportQuery.dedup = "FIRST_ISOLATE_7_DAY";
        exportQuery.page = 1;
        exportQuery.pageSize = 20;

        WHONetReportServiceImpl reportService = new WHONetReportServiceImpl(datasetService, exportRunDAO);
        MicroWhonetPreviewForm preview = reportService.previewMicrobiologyExport(exportQuery);
        assertTrue(preview.rows.stream().anyMatch(row -> scenario.accessionNumber.equals(row.accessionNumber)));

        MicroWhonetExportResult result = reportService.generateMicrobiologyExport(exportQuery, performedBy);
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(result.getContent()));
        MicroWhonetExportRun persisted = exportRunDAO.getAll().stream()
                .filter(candidate -> digest.equals(candidate.getContentSha256())).findFirst().orElseThrow();

        assertEquals(performedBy, persisted.getGeneratedBy());
        assertEquals(result.getFileName(), persisted.getFileName());
        assertEquals(preview.totalCases, persisted.getCaseCount());
        assertEquals(preview.exportableIsolates, persisted.getIsolateCount());
        assertEquals(preview.exportedRows, persisted.getRowCount());
        assertEquals(preview.excludedRows, persisted.getExcludedRowCount());
        assertEquals(List.of(scenario.sampleTypeId), persisted.getPopulationSelection().getSpecimen());
        assertEquals(List.of(scenario.organismId), persisted.getPopulationSelection().getOrganism());
        assertTrue(persisted.getPopulationSelection().getOrigin().isEmpty());
        assertEquals(List.of(MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name()),
                persisted.getPopulationSelection().getSignificance());
        assertTrue(new String(result.getContent(), java.nio.charset.StandardCharsets.UTF_8)
                .contains(scenario.accessionNumber));
    }

    @Test
    public void independentWhonetScenariosCreateDistinctSchemaValidSampleTypes() {
        String performedBy = fixtures.defaultUserId();
        MicrobiologyUatScenarioRequestForm request = new MicrobiologyUatScenarioRequestForm();
        request.scenario = "M4";
        request.scenarioKey = "integration-m4-first-" + UUID.randomUUID();
        MicrobiologyUatScenarioForm first = uatScenarioService.provision(request, performedBy);

        request.scenarioKey = "integration-m4-second-" + UUID.randomUUID();
        MicrobiologyUatScenarioForm second = uatScenarioService.provision(request, performedBy);

        TypeOfSample firstSampleType = typeOfSampleService.get(first.sampleTypeId);
        TypeOfSample secondSampleType = typeOfSampleService.get(second.sampleTypeId);
        assertFalse(firstSampleType.getId().equals(secondSampleType.getId()));
        assertFalse(firstSampleType.getLocalAbbreviation().equals(secondSampleType.getLocalAbbreviation()));
        assertTrue(firstSampleType.getLocalAbbreviation().length() <= 10);
        assertTrue(secondSampleType.getLocalAbbreviation().length() <= 10);
    }
}
