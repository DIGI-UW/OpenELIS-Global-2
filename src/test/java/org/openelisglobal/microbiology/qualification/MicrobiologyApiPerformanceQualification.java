package org.openelisglobal.microbiology.qualification;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.service.MicroAstService;
import org.openelisglobal.microbiology.service.MicroBreakpointService;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroIsolateService;
import org.openelisglobal.microbiology.service.MicrobiologyConfigurationService;
import org.openelisglobal.microbiology.service.MicrobiologyQualificationDataService;
import org.openelisglobal.microbiology.service.MicrobiologyReferenceService;
import org.openelisglobal.microbiology.service.MicrobiologyUatScenarioService;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.SpringVersion;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Explicit qualification runner. Its name intentionally does not match the
 * default Surefire test patterns; invoke it alone against the disposable test
 * database with {@code -Dtest=MicrobiologyApiPerformanceQualification}.
 */
public class MicrobiologyApiPerformanceQualification extends BaseWebContextSensitiveTest {

    private static final int WARMUPS = 5;
    private static final int MEASURED = 20;
    private static final int TOTAL_WRITE_OPERATIONS = WARMUPS + MEASURED;

    @Autowired
    private MicrobiologyUatScenarioService scenarioService;

    @Autowired
    private MicrobiologyReferenceService referenceService;

    @Autowired
    private MicroBreakpointService breakpointService;

    @Autowired
    private MicrobiologyConfigurationService configurationService;

    @Autowired
    private MicroIsolateService isolateService;

    @Autowired
    private MicroAstService astService;

    @Autowired
    private MicroCaseService caseService;

    @Autowired
    private MicrobiologyTestFixtures fixtures;

    @Autowired
    private DataSource dataSource;

    @Test
    public void qualifyApiOperationsAtExpectedLaboratoryVolume() throws Exception {
        String runKey = UUID.randomUUID().toString();
        String userId = fixtures.defaultUserId();
        MicrobiologyQualificationDataService qualificationService = new MicrobiologyQualificationDataService(
                scenarioService, referenceService, breakpointService, configurationService, isolateService, astService,
                caseService, true);
        MicrobiologyQualificationDataService.WorklistDataset worklist = qualificationService.buildWorklist(runKey, 200,
                userId);
        MicrobiologyQualificationDataService.DenseCaseDataset dense = qualificationService.buildDenseCase(runKey,
                userId);
        UserSessionData session = sessionFor(userId);

        List<PerformanceEvidence.Measurement> measurements = new ArrayList<>();
        measurements
                .add(PerformanceEvidence
                        .measure("worklist-load", WARMUPS, MEASURED, 2000,
                                (iteration,
                                        warmup) -> expectOk(
                                                authenticated(
                                                        get("/rest/microbiology/worklist")
                                                                .param("workflow",
                                                                        MicroWorkflowType.BACTERIOLOGY.name())
                                                                .param("page", "1").param("pageSize", "100"),
                                                        session))));
        measurements.add(PerformanceEvidence.measure("worklist-search", WARMUPS, MEASURED, 500,
                (iteration, warmup) -> expectOk(authenticated(get("/rest/microbiology/worklist")
                        .param("q", dense.caseId()).param("page", "1").param("pageSize", "100"), session))));
        measurements.add(PerformanceEvidence.measure("worklist-filter-page", WARMUPS, MEASURED, 300,
                (iteration, warmup) -> expectOk(authenticated(
                        get("/rest/microbiology/worklist").param("workflow", MicroWorkflowType.BACTERIOLOGY.name())
                                .param("sort", "newest").param("page", "2").param("pageSize", "50"),
                        session))));
        measurements.add(PerformanceEvidence.measure("case-load", WARMUPS, MEASURED, 1000, (iteration,
                warmup) -> expectOk(authenticated(get("/rest/microbiology/cases/{caseId}", dense.caseId()), session))));

        measurements
                .add(PerformanceEvidence.measure("isolate-save", WARMUPS, MEASURED, 500,
                        (iteration, warmup) -> expectOk(authenticated(
                                post("/rest/microbiology/isolates").contentType(MediaType.APPLICATION_JSON)
                                        .content(mapToJson(Map.of("caseId", dense.caseId(), "isolateLabel",
                                                operationKey("QPERF-ISO", iteration, warmup), "preliminaryOrganismText",
                                                "Qualification organism", "significance", "CLINICALLY_SIGNIFICANT"))),
                                session))));

        MicroAstPanel panel = qualificationPanel();
        MicroBreakpointStandard standard = qualificationStandard();
        MicroAntibiotic antibiotic = qualificationAntibiotic();
        List<MicroAstRun> writeRuns = new ArrayList<>(TOTAL_WRITE_OPERATIONS);
        for (int index = 0; index < TOTAL_WRITE_OPERATIONS; index++) {
            writeRuns.add(astService.startRun(dense.isolateIds().get(0), panel.getId(), standard.getId(), userId));
        }
        measurements
                .add(PerformanceEvidence.measure("ast-reading-save", WARMUPS, MEASURED, 500, (iteration, warmup) -> {
                    MicroAstRun run = writeRuns.get(operationIndex(iteration, warmup));
                    expectOk(
                            authenticated(
                                    post("/rest/microbiology/ast/runs/{runId}/readings", run.getId())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(mapToJson(Map.of("antibioticId", antibiotic.getId(), "method",
                                                    MicroAstMethod.MIC.name(), "rawValue", new BigDecimal("4")))),
                                    session));
                }));

        measurements.add(PerformanceEvidence.measure("timeline-save", WARMUPS, MEASURED, 500, (iteration, warmup) -> {
            String caseId = worklist.caseIds().get(operationIndex(iteration, warmup));
            expectOk(authenticated(
                    post("/rest/microbiology/cases/{caseId}/activities", caseId).contentType(MediaType.APPLICATION_JSON)
                            .content(mapToJson(Map.of("nextStage", MicroCaseStage.SETUP_RECORDED.name(), "note",
                                    operationKey("Qualification setup", iteration, warmup)))),
                    session));
        }));

        Map<String, String> environment = PerformanceEvidence.currentEnvironment(databaseVersion());
        environment = new LinkedHashMap<>(environment);
        environment.put("spring", SpringVersion.getVersion());
        environment.put("httpHarness", "Spring MockMvc");
        Map<String, Integer> dataVolume = new LinkedHashMap<>();
        dataVolume.put("worklistCases", worklist.caseIds().size());
        dataVolume.put("denseCaseIsolates", dense.isolateIds().size());
        dataVolume.put("denseCaseReadings", dense.readingCount());
        dataVolume.put("denseCaseTimelineEvents", dense.timelineEventCount());
        PerformanceEvidence.Evidence evidence = PerformanceEvidence.evidence(requireCommit(), environment, dataVolume,
                measurements);
        PerformanceEvidence.OutputPaths output = PerformanceEvidence.write(Path.of("target", "qualification"),
                "microbiology-api-performance", evidence);

        System.out.println("Microbiology API qualification evidence: " + output.json().toAbsolutePath());
        assertTrue("One or more API p95 thresholds failed; inspect " + output.json().toAbsolutePath(),
                evidence.passed());
    }

    private MicroAstPanel qualificationPanel() {
        return referenceService.getActiveAstPanels(MicroWorkflowType.BACTERIOLOGY).stream()
                .filter(panel -> "Gram negative AST panel (UAT)".equals(panel.getName())).findFirst().orElseThrow();
    }

    private MicroBreakpointStandard qualificationStandard() {
        return breakpointService.getActiveStandards().stream()
                .filter(standard -> "CLSI".equals(standard.getAuthority()) && "2026".equals(standard.getVersion()))
                .findFirst().orElseThrow();
    }

    private MicroAntibiotic qualificationAntibiotic() {
        return referenceService.getActiveAntibiotics().stream()
                .filter(antibiotic -> "QAST01".equals(antibiotic.getWhonetCode())).findFirst().orElseThrow();
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request,
            UserSessionData session) {
        return request.sessionAttr(IActionConstants.USER_SESSION_DATA, session);
    }

    private void expectOk(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isOk());
    }

    private UserSessionData sessionFor(String userId) {
        UserSessionData session = new UserSessionData();
        session.setSytemUserId(Integer.parseInt(userId));
        return session;
    }

    private int operationIndex(int iteration, boolean warmup) {
        return warmup ? iteration : WARMUPS + iteration;
    }

    private String operationKey(String prefix, int iteration, boolean warmup) {
        return prefix + "-" + (warmup ? "W" : "M") + String.format(Locale.ROOT, "%02d", iteration + 1);
    }

    private String databaseVersion() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductVersion();
        }
    }

    private String requireCommit() {
        String commit = System.getProperty("ogc782.commit", System.getenv("GITHUB_SHA"));
        if (commit == null || commit.trim().isEmpty()) {
            throw new IllegalStateException("Set -Dogc782.commit=<git-sha> so evidence is traceable");
        }
        return commit;
    }
}
