package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroOrderRoutingService;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MicroOrderRoutingIdempotencyTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MicrobiologyTestFixtures fixtures;

    @Autowired
    private MicroOrderRoutingService routingService;

    @Autowired
    private MicroCaseService caseService;

    private String sampleItemId;
    private String methodId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        methodId = fixtures.createMethodId();
        sampleItemId = fixtures.createSampleWithSampleItem("OGC782M3I").getId();
        fixtures.createReferenceData(methodId);
    }

    @Test
    public void repeatedRoutingDoesNotDuplicateCases() {
        Analysis bacteriology = analysis(MicroWorkflowType.BACTERIOLOGY.name(), methodId);

        routingService.routeAnalysesForSampleItem(sampleItem(sampleItemId), List.of(bacteriology),
                fixtures.defaultUserId());
        routingService.routeAnalysesForSampleItem(sampleItem(sampleItemId), List.of(bacteriology),
                fixtures.defaultUserId());

        assertEquals(1, caseService.getSiblingCases(sampleItemId).size());
    }

    private SampleItem sampleItem(String id) {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId(id);
        return sampleItem;
    }

    private Analysis analysis(String workflowType, String methodId) {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId("test-" + workflowType + "-" + methodId);
        test.setCultureWorkflowType(workflowType);
        Method method = new Method();
        method.setId(methodId);
        test.setMethod(method);
        Analysis analysis = new Analysis();
        analysis.setTest(test);
        return analysis;
    }
}
