package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import java.sql.Date;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures.AlternativeBreakpointData;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures.ReferenceData;
import org.openelisglobal.microbiology.service.MicroAstService;
import org.openelisglobal.microbiology.service.MicroBreakpointAdminService;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroIsolateService;
import org.openelisglobal.microbiology.valueholder.MicroAstAttemptType;
import org.openelisglobal.microbiology.valueholder.MicroAstInterpretation;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunStatus;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MicroAstIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MicrobiologyTestFixtures fixtures;

    @Autowired
    private MicroCaseService caseService;

    @Autowired
    private MicroIsolateService isolateService;

    @Autowired
    private MicroAstService astService;

    @Autowired
    private MicroBreakpointAdminService breakpointAdminService;

    private String sampleItemId;
    private String methodId;
    private ReferenceData referenceData;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        methodId = fixtures.createMethodId();
        sampleItemId = fixtures.createSampleWithSampleItem("OGC782M5").getId();
        referenceData = fixtures.createReferenceData(methodId);
        breakpointAdminService.activate(referenceData.standard().getId(), new Date(System.currentTimeMillis()),
                fixtures.defaultUserId());
    }

    @Test
    public void astRunInterpretsAgainstItsSnapshottedBreakpointStandard() {
        AlternativeBreakpointData alternative = fixtures.createAlternativeBreakpoint(referenceData);
        MicroCase microCase = caseService.createOrGetCase(sampleItemId, MicroWorkflowType.BACTERIOLOGY, methodId,
                fixtures.defaultUserId());
        MicroIsolate isolate = isolateService.createIsolate(microCase.getId(), "ISO-1",
                referenceData.organism().getId(), referenceData.organism().getDisplayName(),
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, fixtures.defaultUserId());

        MicroAstRun defaultRun = astService.startRun(isolate.getId(), referenceData.panel().getId(),
                fixtures.defaultUserId());
        MicroAstReading defaultReading = astService.recordReading(defaultRun.getId(),
                referenceData.antibiotic().getId(), MicroAstMethod.MIC, new BigDecimal("4"), fixtures.defaultUserId());

        MicroAstRun altRun = astService.startRun(isolate.getId(), referenceData.panel().getId(),
                alternative.standard().getId(), fixtures.defaultUserId());
        MicroAstReading altReading = astService.recordReading(altRun.getId(), referenceData.antibiotic().getId(),
                MicroAstMethod.MIC, new BigDecimal("4"), fixtures.defaultUserId());

        assertEquals(alternative.standard().getId(), altRun.getBreakpointStandardId());
        assertEquals(MicroAstInterpretation.SUSCEPTIBLE.name(), defaultReading.getInterpretation());
        assertEquals(MicroAstInterpretation.INTERMEDIATE.name(), altReading.getInterpretation());
    }

    @Test
    public void astRunStoresReadingsInterpretationOverrideAndReview() {
        MicroCase microCase = caseService.createOrGetCase(sampleItemId, MicroWorkflowType.BACTERIOLOGY, methodId,
                fixtures.defaultUserId());
        MicroIsolate isolate = isolateService.createIsolate(microCase.getId(), "ISO-1",
                referenceData.organism().getId(), referenceData.organism().getDisplayName(),
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, fixtures.defaultUserId());

        MicroAstRun run = astService.startRun(isolate.getId(), referenceData.panel().getId(), fixtures.defaultUserId());
        MicroAstReading reading = astService.recordReading(run.getId(), referenceData.antibiotic().getId(),
                MicroAstMethod.MIC, new BigDecimal("4"), fixtures.defaultUserId());
        MicroAstReading overridden = astService.overrideReading(reading.getId(), MicroAstInterpretation.RESISTANT,
                "mixed growth confirmed on repeat", fixtures.defaultUserId());
        MicroAstRun reviewed = astService.reviewRun(run.getId(), fixtures.defaultUserId());

        assertNotNull(reading.getBreakpointRuleId());
        assertEquals(MicroAstInterpretation.SUSCEPTIBLE.name(), reading.getInterpretation());
        assertEquals(MicroAstInterpretation.RESISTANT.name(), overridden.getOverrideInterpretation());
        assertEquals(MicroAstRunStatus.REVIEWED.name(), reviewed.getStatus());
    }

    @Test
    public void repeatAttemptPersistsProvenanceAndExplicitReportableSelection() {
        MicroCase microCase = caseService.createOrGetCase(sampleItemId, MicroWorkflowType.BACTERIOLOGY, methodId,
                fixtures.defaultUserId());
        MicroIsolate isolate = isolateService.createIsolate(microCase.getId(), "ISO-1",
                referenceData.organism().getId(), referenceData.organism().getDisplayName(),
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, fixtures.defaultUserId());
        MicroAstRun original = astService.startRun(isolate.getId(), referenceData.panel().getId(),
                fixtures.defaultUserId());
        astService.recordReading(original.getId(), referenceData.antibiotic().getId(), MicroAstMethod.MIC,
                new BigDecimal("4"), fixtures.defaultUserId());
        original = astService.reviewRun(original.getId(), fixtures.defaultUserId());

        MicroAstRun repeat = astService.startRepeatRun(original.getId(), MicroAstAttemptType.REPEAT,
                "Discordant result on repeat culture", MicroAstMethod.MIC, fixtures.defaultUserId());
        astService.recordReading(repeat.getId(), referenceData.antibiotic().getId(), MicroAstMethod.MIC,
                new BigDecimal("40"), fixtures.defaultUserId());
        repeat = astService.reviewRun(repeat.getId(), fixtures.defaultUserId());

        assertEquals(original.getId(), repeat.getSourceRunId());
        assertEquals(MicroAstAttemptType.REPEAT.name(), repeat.getAttemptType());
        assertEquals("Discordant result on repeat culture", repeat.getAttemptReason());
        assertEquals(MicroAstMethod.MIC.name(), repeat.getMethod());
        assertEquals(false, original.isReportable());
        assertEquals(false, repeat.isReportable());

        MicroAstRun selected = astService.selectReportableRun(repeat.getId(), fixtures.defaultUserId());
        assertEquals(true, selected.isReportable());
        assertEquals(true, astService.selectReportableRun(original.getId(), fixtures.defaultUserId()).isReportable());
        assertEquals(true, astService.selectReportableRun(repeat.getId(), fixtures.defaultUserId()).isReportable());
        assertEquals(2, astService.getRunsForIsolate(isolate.getId()).size());
    }
}
