package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.service.MicroAstService;
import org.openelisglobal.microbiology.service.MicroBreakpointService;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroIsolateService;
import org.openelisglobal.microbiology.service.MicrobiologyConfigurationService;
import org.openelisglobal.microbiology.service.MicrobiologyQualificationDataService;
import org.openelisglobal.microbiology.service.MicrobiologyReferenceService;
import org.openelisglobal.microbiology.service.MicrobiologyUatScenarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MicrobiologyQualificationDataIntegrationTest extends BaseWebContextSensitiveTest {

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

    @Test
    public void denseQualificationDatasetIsServiceCreatedAndRolledBackWithTheTestTransaction() {
        fixtures.ensureRequiredWorkflowStatuses();
        MicrobiologyQualificationDataService qualificationService = new MicrobiologyQualificationDataService(
                scenarioService, referenceService, breakpointService, configurationService, isolateService, astService,
                caseService, true);

        MicrobiologyQualificationDataService.DenseCaseDataset dataset = qualificationService
                .buildDenseCase(UUID.randomUUID().toString(), fixtures.defaultUserId());

        assertEquals(5, dataset.isolateIds().size());
        assertEquals(80, dataset.readingCount());
        assertTrue(dataset.timelineEventCount() >= 91);
        assertEquals(5, caseService.getCaseDetail(dataset.caseId()).isolates.size());
    }
}
