package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures.ReferenceData;
import org.openelisglobal.microbiology.form.MicroCaseDetailForm;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroCaseStateService;
import org.openelisglobal.microbiology.service.MicroIsolateService;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MicroCaseIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MicrobiologyTestFixtures fixtures;

    @Autowired
    private MicroCaseService caseService;

    @Autowired
    private MicroCaseStateService stateService;

    @Autowired
    private MicroIsolateService isolateService;

    private String sampleItemId;
    private String methodId;
    private ReferenceData referenceData;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        methodId = fixtures.firstMethodId();
        sampleItemId = fixtures.createSampleWithSampleItem("OGC782M2").getId();
        referenceData = fixtures.createReferenceData(methodId);
    }

    @Test
    public void caseIdentityIsUniquePerSampleItemAndWorkflowWithSiblingSupport() {
        MicroCase first = caseService.createOrGetCase(sampleItemId, MicroWorkflowType.BACTERIOLOGY, methodId,
                MicrobiologyTestFixtures.DEFAULT_USER_ID);
        MicroCase duplicate = caseService.createOrGetCase(sampleItemId, MicroWorkflowType.BACTERIOLOGY, methodId,
                MicrobiologyTestFixtures.DEFAULT_USER_ID);
        MicroCase sibling = caseService.createOrGetCase(sampleItemId, MicroWorkflowType.MYCOBACTERIOLOGY_TB, methodId,
                MicrobiologyTestFixtures.DEFAULT_USER_ID);

        assertEquals(first.getId(), duplicate.getId());
        assertEquals(sampleItemId, sibling.getSampleItemId());
        assertEquals(2, caseService.getSiblingCases(sampleItemId).size());
    }

    @Test
    public void compiledCaseDetailIncludesTimelineAndIsolatesWithoutControllerTraversal() {
        MicroCase microCase = caseService.createOrGetCase(sampleItemId, MicroWorkflowType.BACTERIOLOGY, methodId,
                MicrobiologyTestFixtures.DEFAULT_USER_ID);
        stateService.advanceStage(microCase.getId(), MicroCaseStage.SETUP_RECORDED,
                MicrobiologyTestFixtures.DEFAULT_USER_ID, "setup complete");
        isolateService.createIsolate(microCase.getId(), "ISO-1", referenceData.organism().getId(),
                referenceData.organism().getDisplayName(), MicroIsolateSignificance.CLINICALLY_SIGNIFICANT,
                MicrobiologyTestFixtures.DEFAULT_USER_ID);

        MicroCaseDetailForm detail = caseService.getCaseDetail(microCase.getId());

        assertEquals(microCase.getId(), detail.id);
        assertEquals(1, detail.isolates.size());
        assertEquals("ISO-1", detail.isolates.get(0).isolateLabel);
        assertTrue(detail.activities.size() >= 3);
    }

}
