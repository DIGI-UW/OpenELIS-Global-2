package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioForm;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioRequestForm;
import org.openelisglobal.microbiology.service.MicroAstService;
import org.openelisglobal.microbiology.service.MicroBreakpointService;
import org.openelisglobal.microbiology.service.MicroCaseAmendmentService;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroIdentificationHistoryService;
import org.openelisglobal.microbiology.service.MicroIsolateService;
import org.openelisglobal.microbiology.service.MicroReportReleaseService;
import org.openelisglobal.microbiology.service.MicroReportVersionService;
import org.openelisglobal.microbiology.service.MicrobiologyReferenceService;
import org.openelisglobal.microbiology.service.MicrobiologyUatScenarioService;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseFinalReleaseState;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationEvent;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.openelisglobal.microbiology.valueholder.MicroReportVersion;
import org.openelisglobal.microbiology.valueholder.MicroReportVersionType;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class MicroAmendmentIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MicrobiologyUatScenarioService scenarioService;

    @Autowired
    private MicrobiologyReferenceService referenceService;

    @Autowired
    private MicroBreakpointService breakpointService;

    @Autowired
    private MicroIsolateService isolateService;

    @Autowired
    private MicroAstService astService;

    @Autowired
    private MicroReportReleaseService reportReleaseService;

    @Autowired
    private MicroCaseAmendmentService amendmentService;

    @Autowired
    private MicroReportVersionService reportVersionService;

    @Autowired
    private MicroIdentificationHistoryService identificationHistoryService;

    @Autowired
    private MicroCaseService caseService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    public void amendmentAppendsIdentificationAndReportHistoryThenRelocks() {
        FinalCase fixture = createFinalCase();

        amendmentService.openAmendment(fixture.caseId(), "Correct identification", fixture.userId());
        try {
            amendmentService.openAmendment(fixture.caseId(), "Open a second amendment", fixture.userId());
            fail("Expected only one open amendment per case");
        } catch (RuntimeException expected) {
            assertEquals("AMENDMENT_ALREADY_OPEN", expected.getMessage());
        }

        isolateService.updateIdentification(fixture.isolateId(), null, "Klebsiella pneumoniae",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicroIsolateIdentificationStatus.CONFIRMED,
                "Confirmatory identification corrected the organism", fixture.userId());
        reportReleaseService.releaseAmended(fixture.caseId(), fixture.userId());

        List<MicroReportVersion> versions = reportVersionService.getVersions(fixture.caseId());
        assertEquals(2, versions.size());
        assertEquals(Integer.valueOf(1), versions.get(0).getVersionNumber());
        assertEquals(MicroReportVersionType.FINAL.name(), versions.get(0).getReleaseType());
        assertTrue(versions.get(0).getContent().contains("Escherichia coli"));
        assertEquals(Integer.valueOf(2), versions.get(1).getVersionNumber());
        assertEquals(MicroReportVersionType.AMENDED.name(), versions.get(1).getReleaseType());
        assertEquals(versions.get(0).getId(), versions.get(1).getCorrectsVersionId());
        assertTrue(versions.get(1).getContent().contains("Klebsiella pneumoniae"));
        assertTrue(reportVersionService.getSourcesForCase(fixture.caseId()).size() >= 2);

        List<MicroIsolateIdentificationEvent> history = identificationHistoryService.getHistory(fixture.isolateId());
        assertEquals(1, history.size());
        assertEquals("Escherichia coli", history.get(0).getPreviousOrganismText());
        assertEquals("Klebsiella pneumoniae", history.get(0).getNewOrganismText());
        assertEquals("Confirmatory identification corrected the organism", history.get(0).getReason());

        MicroCase relocked = caseService.getCase(fixture.caseId());
        assertEquals(MicroCaseStage.FINAL_RELEASED.name(), relocked.getStage());
        assertEquals(MicroCaseFinalReleaseState.FINAL_RELEASED.name(), relocked.getFinalReleaseState());
        assertNull(amendmentService.getOpenAmendment(fixture.caseId()));
    }

    @Test
    public void outerFailureRollsBackTheEntireAmendmentOpenTransaction() {
        FinalCase fixture = createFinalCase();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        try {
            transaction.executeWithoutResult(status -> {
                amendmentService.openAmendment(fixture.caseId(), "Must roll back", fixture.userId());
                throw new IllegalStateException("FORCED_ROLLBACK");
            });
            fail("Expected the transaction to roll back");
        } catch (IllegalStateException expected) {
            assertEquals("FORCED_ROLLBACK", expected.getMessage());
        }

        assertTrue(amendmentService.getHistory(fixture.caseId()).isEmpty());
        assertEquals(1, reportVersionService.getVersions(fixture.caseId()).size());
        MicroCase unchanged = caseService.getCase(fixture.caseId());
        assertEquals(MicroCaseStage.FINAL_RELEASED.name(), unchanged.getStage());
        assertEquals(MicroCaseFinalReleaseState.FINAL_RELEASED.name(), unchanged.getFinalReleaseState());
    }

    private FinalCase createFinalCase() {
        String userId = fixturesUserId();
        MicrobiologyUatScenarioRequestForm request = new MicrobiologyUatScenarioRequestForm();
        request.scenario = "MVP";
        request.scenarioKey = "amendment-integration-" + UUID.randomUUID();
        MicrobiologyUatScenarioForm scenario = scenarioService.provision(request, userId);
        MicroIsolate isolate = isolateService.createIsolate(scenario.caseId, "ISO-1", null, "Escherichia coli",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, userId);

        String panelId = referenceService.getActiveAstPanels(MicroWorkflowType.BACTERIOLOGY).stream()
                .filter(panel -> "Gram negative AST panel (UAT)".equals(panel.getName())).findFirst().orElseThrow()
                .getId();
        String standardId = breakpointService.getActiveStandard("CLSI", "2026").getId();
        MicroAntibiotic antibiotic = referenceService.getActiveAntibiotics().stream()
                .filter(candidate -> "CIPUAT".equals(candidate.getWhonetCode())).findFirst().orElseThrow();
        MicroAstRun run = astService.startRun(isolate.getId(), panelId, standardId, userId);
        astService.recordReading(run.getId(), antibiotic.getId(), MicroAstMethod.MIC, new BigDecimal("4"), userId);
        astService.reviewRun(run.getId(), userId);
        reportReleaseService.releaseFinal(scenario.caseId, userId);
        return new FinalCase(scenario.caseId, isolate.getId(), userId);
    }

    private String fixturesUserId() {
        return webApplicationContext.getBean(org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures.class)
                .defaultUserId();
    }

    private record FinalCase(String caseId, String isolateId, String userId) {
    }
}
