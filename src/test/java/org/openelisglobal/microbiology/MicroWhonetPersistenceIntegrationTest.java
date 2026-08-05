package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioForm;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioRequestForm;
import org.openelisglobal.microbiology.service.MicroAstService;
import org.openelisglobal.microbiology.service.MicroIsolateService;
import org.openelisglobal.microbiology.service.MicroReportReleaseService;
import org.openelisglobal.microbiology.service.MicroWhonetDatasetService;
import org.openelisglobal.microbiology.service.MicrobiologyReferenceService;
import org.openelisglobal.microbiology.service.MicrobiologyUatScenarioService;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.openelisglobal.microbiology.valueholder.MicroWhonetExportRun;
import org.openelisglobal.reports.service.MicroWhonetExportResult;
import org.openelisglobal.reports.service.WHONetReportServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MicroWhonetPersistenceIntegrationTest extends BaseWebContextSensitiveTest {

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
    private MicrobiologyReferenceService referenceService;

    @Autowired
    private MicroCaseDAO caseDAO;

    @Autowired
    private MicroWhonetExportRunDAO exportRunDAO;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        fixtures.ensureRequiredWorkflowStatuses();
    }

    @Test
    public void serviceCreatedFinalCaseIsSelectedAndExportAuditRoundTrips() throws Exception {
        String performedBy = fixtures.defaultUserId();
        MicrobiologyUatScenarioRequestForm request = new MicrobiologyUatScenarioRequestForm();
        request.scenario = "M4";
        request.scenarioKey = "integration-m4-" + UUID.randomUUID();
        MicrobiologyUatScenarioForm scenario = uatScenarioService.provision(request, performedBy);

        MicroIsolate isolate = isolateService.createIsolate(scenario.caseId, "WHONET-INTEGRATION", scenario.organismId,
                "Reference organism (integration)", MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, performedBy);
        MicroAntibiotic exportAntibiotic = referenceService.getActiveAntibiotics().stream()
                .filter(candidate -> "CIPUAT".equals(candidate.getWhonetCode())).findFirst()
                .orElseThrow(() -> new IllegalStateException("M4 export antibiotic was not provisioned"));
        MicroAstRun run = astService.startRun(isolate.getId(), scenario.astPanelId, scenario.activeBreakpointStandardId,
                performedBy);
        astService.recordReading(run.getId(), exportAntibiotic.getId(), MicroAstMethod.MIC, new BigDecimal("4"),
                performedBy);
        astService.reviewRun(run.getId(), performedBy);
        MicroCase released = reportReleaseService.releaseFinal(scenario.caseId, performedBy);

        Timestamp closedAt = released.getClosedAt();
        assertEquals(List.of(released),
                caseDAO.getFinalizedBacteriologyByClosedAtRange(closedAt, new Timestamp(closedAt.getTime() + 1_000)));
        assertFalse(caseDAO.getFinalizedBacteriologyByClosedAtRange(new Timestamp(closedAt.getTime() - 1_000), closedAt)
                .contains(released));

        LocalDate exportDate = closedAt.toLocalDateTime().toLocalDate();
        MicroWhonetExportQueryForm exportQuery = new MicroWhonetExportQueryForm();
        exportQuery.from = exportDate.toString();
        exportQuery.to = exportDate.toString();
        exportQuery.significance = MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name();
        exportQuery.dedup = "FIRST_ISOLATE_7_DAY";
        exportQuery.page = 1;
        exportQuery.pageSize = 20;

        MicroWhonetExportResult result = new WHONetReportServiceImpl(datasetService, exportRunDAO)
                .generateMicrobiologyExport(exportQuery, performedBy);
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(result.content));
        MicroWhonetExportRun persisted = exportRunDAO.getAll().stream()
                .filter(candidate -> digest.equals(candidate.getContentSha256())).findFirst().orElseThrow();

        assertEquals(performedBy, persisted.getGeneratedBy());
        assertEquals(result.fileName, persisted.getFileName());
        assertEquals(1, persisted.getCaseCount());
        assertEquals(1, persisted.getIsolateCount());
        assertEquals(1, persisted.getRowCount());
        assertEquals(0, persisted.getExcludedRowCount());
        assertTrue(
                new String(result.content, java.nio.charset.StandardCharsets.UTF_8).contains(scenario.accessionNumber));
    }
}
