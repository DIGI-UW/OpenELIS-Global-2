package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures.AlternativeBreakpointData;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures.ReferenceData;
import org.openelisglobal.microbiology.service.MicroAstService;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroIsolateService;
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

    private String sampleItemId;
    private String methodId;
    private ReferenceData referenceData;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        methodId = fixtures.firstMethodId();
        sampleItemId = fixtures.createSampleWithSampleItem("OGC782M5").getId();
        referenceData = fixtures.createReferenceData(methodId);
    }

    @Test
    public void astRunInterpretsAgainstItsSnapshottedBreakpointStandard() {
        AlternativeBreakpointData alternative = fixtures.createAlternativeBreakpoint(referenceData);
        MicroCase microCase = caseService.createOrGetCase(sampleItemId, MicroWorkflowType.BACTERIOLOGY, methodId,
                MicrobiologyTestFixtures.DEFAULT_USER_ID);
        MicroIsolate isolate = isolateService.createIsolate(microCase.getId(), "ISO-1",
                referenceData.organism().getId(), referenceData.organism().getDisplayName(),
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicrobiologyTestFixtures.DEFAULT_USER_ID);

        MicroAstRun defaultRun = astService.startRun(isolate.getId(), referenceData.panel().getId(),
                MicrobiologyTestFixtures.DEFAULT_USER_ID);
        MicroAstReading defaultReading = astService.recordReading(defaultRun.getId(),
                referenceData.antibiotic().getId(), MicroAstMethod.MIC, new BigDecimal("4"),
                MicrobiologyTestFixtures.DEFAULT_USER_ID);

        MicroAstRun altRun = astService.startRun(isolate.getId(), referenceData.panel().getId(),
                alternative.standard().getId(), MicrobiologyTestFixtures.DEFAULT_USER_ID);
        MicroAstReading altReading = astService.recordReading(altRun.getId(), referenceData.antibiotic().getId(),
                MicroAstMethod.MIC, new BigDecimal("4"), MicrobiologyTestFixtures.DEFAULT_USER_ID);

        assertEquals(alternative.standard().getId(), altRun.getBreakpointStandardId());
        assertEquals(MicroAstInterpretation.SUSCEPTIBLE.name(), defaultReading.getInterpretation());
        assertEquals(MicroAstInterpretation.INTERMEDIATE.name(), altReading.getInterpretation());
    }

    @Test
    public void astRunStoresReadingsInterpretationOverrideAndReview() {
        MicroCase microCase = caseService.createOrGetCase(sampleItemId, MicroWorkflowType.BACTERIOLOGY, methodId,
                MicrobiologyTestFixtures.DEFAULT_USER_ID);
        MicroIsolate isolate = isolateService.createIsolate(microCase.getId(), "ISO-1",
                referenceData.organism().getId(), referenceData.organism().getDisplayName(),
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicrobiologyTestFixtures.DEFAULT_USER_ID);

        MicroAstRun run = astService.startRun(isolate.getId(), referenceData.panel().getId(),
                MicrobiologyTestFixtures.DEFAULT_USER_ID);
        MicroAstReading reading = astService.recordReading(run.getId(), referenceData.antibiotic().getId(),
                MicroAstMethod.MIC, new BigDecimal("4"), MicrobiologyTestFixtures.DEFAULT_USER_ID);
        MicroAstReading overridden = astService.overrideReading(reading.getId(), MicroAstInterpretation.RESISTANT,
                "mixed growth confirmed on repeat", MicrobiologyTestFixtures.DEFAULT_USER_ID);
        MicroAstRun reviewed = astService.reviewRun(run.getId(), MicrobiologyTestFixtures.DEFAULT_USER_ID);

        assertNotNull(reading.getBreakpointRuleId());
        assertEquals(MicroAstInterpretation.SUSCEPTIBLE.name(), reading.getInterpretation());
        assertEquals(MicroAstInterpretation.RESISTANT.name(), overridden.getOverrideInterpretation());
        assertEquals(MicroAstRunStatus.REVIEWED.name(), reviewed.getStatus());
    }
}
