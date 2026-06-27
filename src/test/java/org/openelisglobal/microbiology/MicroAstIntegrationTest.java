package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
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
import org.springframework.jdbc.core.JdbcTemplate;

public class MicroAstIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private javax.sql.DataSource dataSource;

    @Autowired
    private MicroCaseService caseService;

    @Autowired
    private MicroIsolateService isolateService;

    @Autowired
    private MicroAstService astService;

    private MicrobiologyTestFixtures fixtures;
    private String sampleItemId;
    private String methodId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        fixtures = new MicrobiologyTestFixtures(new JdbcTemplate(dataSource));
        methodId = fixtures.firstMethodId();
        sampleItemId = fixtures.insertSampleWithSampleItem("OGC782-M5-" + System.nanoTime());
        fixtures.insertReferenceData(methodId);
    }

    @After
    public void tearDown() {
        if (fixtures != null && sampleItemId != null) {
            fixtures.deleteCaseDataForSampleItem(sampleItemId);
            fixtures.deleteSampleItemAndSample(sampleItemId);
            fixtures.deleteReferenceData();
        }
    }

    @Test
    public void astRunStoresReadingsInterpretationOverrideAndReview() {
        MicroCase microCase = caseService.createOrGetCase(sampleItemId, MicroWorkflowType.BACTERIOLOGY, methodId,
                MicrobiologyTestFixtures.DEFAULT_USER_ID);
        MicroIsolate isolate = isolateService.createIsolate(microCase.getId(), "ISO-1",
                MicrobiologyTestFixtures.ORGANISM_ID, "Escherichia coli",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicrobiologyTestFixtures.DEFAULT_USER_ID);

        MicroAstRun run = astService.startRun(isolate.getId(), MicrobiologyTestFixtures.PANEL_ID,
                MicrobiologyTestFixtures.DEFAULT_USER_ID);
        MicroAstReading reading = astService.recordReading(run.getId(), MicrobiologyTestFixtures.ANTIBIOTIC_ID,
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
