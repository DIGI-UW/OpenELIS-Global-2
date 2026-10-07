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
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
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
    private org.openelisglobal.test.valueholder.Test cultureTest;
    private org.openelisglobal.test.valueholder.Test otherUnitTest;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        methodId = fixtures.createMethodId();
        sampleItemId = fixtures.createSampleWithSampleItem("OGC782M2").getId();
        referenceData = fixtures.createReferenceData(methodId);
        cultureTest = fixtures.createCatalogCultureTest(methodId, fixtures.createLabUnit());
        otherUnitTest = fixtures.createCatalogCultureTest(methodId, fixtures.createLabUnit());
    }

    @Test
    public void caseIdentityUsesOrderSampleTypeAndLabUnitWithSeparateRelatedCases() {
        MicroCase first = caseService.createOrGetCase(fixtures.getSampleItem(sampleItemId), cultureTest,
                fixtures.defaultUserId());
        MicroCase duplicate = caseService.createOrGetCase(fixtures.getSampleItem(sampleItemId), cultureTest,
                fixtures.defaultUserId());
        MicroCase sibling = caseService.createOrGetCase(fixtures.getSampleItem(sampleItemId), otherUnitTest,
                fixtures.defaultUserId());

        assertEquals(first.getId(), duplicate.getId());
        assertEquals(java.util.List.of(sampleItemId), caseService.getSpecimenIds(sibling.getId()));
        assertEquals(2, caseService.getSiblingCases(sampleItemId).size());
    }

    @Test
    public void compiledCaseDetailIncludesTimelineAndIsolatesWithoutControllerTraversal() {
        MicroCase microCase = caseService.createOrGetCase(fixtures.getSampleItem(sampleItemId), cultureTest,
                fixtures.defaultUserId());
        stateService.advanceStage(microCase.getId(), MicroCaseStage.SETUP_RECORDED, fixtures.defaultUserId(),
                "setup complete");
        var isolate = isolateService.createIsolate(microCase.getId(), sampleItemId, "ISO-1", "Gram negative rods",
                "Lactose fermenting colonies", MicroIsolateSignificance.CLINICALLY_SIGNIFICANT,
                fixtures.defaultUserId());
        isolateService.updateIdentification(isolate.getId(), referenceData.organism().getId(),
                referenceData.organism().getDisplayName(), MicroIsolateSignificance.CLINICALLY_SIGNIFICANT,
                MicroIsolateIdentificationStatus.CONFIRMED, "MALDI_TOF", new java.math.BigDecimal("99.5"),
                fixtures.defaultUserId());

        MicroCaseDetailForm detail = caseService.getCaseDetail(microCase.getId());

        assertEquals(microCase.getId(), detail.id);
        assertEquals(1, detail.isolates.size());
        assertEquals("ISO-1", detail.isolates.get(0).isolateLabel);
        assertTrue(detail.activities.size() >= 3);
    }

}
