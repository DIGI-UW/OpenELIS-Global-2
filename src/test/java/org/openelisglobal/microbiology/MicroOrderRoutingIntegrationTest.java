package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.After;
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
import org.springframework.jdbc.core.JdbcTemplate;

public class MicroOrderRoutingIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private javax.sql.DataSource dataSource;

    @Autowired
    private MicroOrderRoutingService routingService;

    @Autowired
    private MicroCaseService caseService;

    private MicrobiologyTestFixtures fixtures;
    private String sampleItemId;
    private String methodId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        fixtures = new MicrobiologyTestFixtures(new JdbcTemplate(dataSource));
        methodId = fixtures.firstMethodId();
        sampleItemId = fixtures.insertSampleWithSampleItem("OGC782-M3");
        fixtures.insertReferenceData(methodId);
        fixtures.insertTbCultureSetup(methodId);
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
    public void routesNonMicroBacteriologyAndSiblingWorkflowCases() {
        routingService.routeAnalysesForSampleItem(sampleItem(sampleItemId), List.of(analysis(null, methodId)),
                MicrobiologyTestFixtures.DEFAULT_USER_ID);
        assertEquals(0, caseService.getSiblingCases(sampleItemId).size());

        routingService.routeAnalysesForSampleItem(sampleItem(sampleItemId),
                List.of(analysis(MicroWorkflowType.BACTERIOLOGY.name(), methodId),
                        analysis(MicroWorkflowType.MYCOBACTERIOLOGY_TB.name(), methodId)),
                MicrobiologyTestFixtures.DEFAULT_USER_ID);

        assertEquals(2, caseService.getSiblingCases(sampleItemId).size());
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
