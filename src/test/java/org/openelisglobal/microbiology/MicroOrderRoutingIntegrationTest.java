package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.form.MicroCaseDetailForm;
import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
import org.openelisglobal.microbiology.service.MicroCaseOrderDetailService;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroOrderRoutingService;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseOrderDetail;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MicroOrderRoutingIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MicrobiologyTestFixtures fixtures;

    @Autowired
    private MicroOrderRoutingService routingService;

    @Autowired
    private MicroCaseService caseService;

    @Autowired
    private MicroCaseOrderDetailService orderDetailService;

    private String sampleItemId;
    private String methodId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        methodId = fixtures.firstMethodId();
        sampleItemId = fixtures.createSampleWithSampleItem("OGC782M3").getId();
        fixtures.createReferenceData(methodId);
        fixtures.createTbCultureSetup(methodId);
    }

    @Test
    public void routesNonMicroBacteriologyAndSiblingWorkflowCases() {
        routingService.routeAnalysesForSampleItem(sampleItem(sampleItemId), List.of(analysis(null, methodId)),
                fixtures.defaultUserId());
        assertEquals(0, caseService.getSiblingCases(sampleItemId).size());

        routingService
                .routeAnalysesForSampleItem(sampleItem(sampleItemId),
                        List.of(analysis(MicroWorkflowType.BACTERIOLOGY.name(), methodId),
                                analysis(MicroWorkflowType.MYCOBACTERIOLOGY_TB.name(), methodId)),
                        fixtures.defaultUserId());

        assertEquals(2, caseService.getSiblingCases(sampleItemId).size());
    }

    @Test
    public void persistedOrderCreatesOneCaseWithTypedDetailsAndRemainsIdempotent() {
        SampleItem persistedItem = fixtures.createSampleWithSampleItem("OGC782M3D");
        org.openelisglobal.test.valueholder.Test cultureTest = fixtures.createCatalogCultureTest(methodId,
                MicroWorkflowType.BACTERIOLOGY);
        Analysis persistedAnalysis = fixtures.createAnalysis(persistedItem, cultureTest);
        MicroCaseOrderDetailRequestForm orderDetail = new MicroCaseOrderDetailRequestForm();
        orderDetail.cultureMethodId = methodId;
        orderDetail.patientOrigin = "EMERGENCY";
        orderDetail.numberOfSets = 2;
        orderDetail.clinicalHistory = "Fever and hypotension";
        orderDetail.antibioticExposure = true;
        orderDetail.criticalNotificationPreference = false;

        List<MicroCase> first = routingService.routeAnalysesForSampleItem(persistedItem, List.of(persistedAnalysis),
                fixtures.defaultUserId(), orderDetail);
        List<MicroCase> repeated = routingService.routeAnalysesForSampleItem(persistedItem, List.of(persistedAnalysis),
                fixtures.defaultUserId(), orderDetail);

        assertEquals(1, first.size());
        assertEquals(first.get(0).getId(), repeated.get(0).getId());
        assertEquals(1, caseService.getSiblingCases(persistedItem.getId()).size());
        MicroCaseOrderDetail persisted = orderDetailService.getOrderDetail(first.get(0).getId());
        assertEquals(Integer.valueOf(2), persisted.getNumberOfSets());
        assertTrue(persisted.getAntibioticExposure());
        assertFalse(persisted.getCriticalNotificationPreference());

        MicroCaseDetailForm compiled = caseService.getCaseDetail(first.get(0).getId());
        assertEquals("EMERGENCY", compiled.orderDetail.patientOrigin);
        assertEquals("Fever and hypotension", compiled.orderDetail.clinicalHistory);
        assertTrue(compiled.orderDetail.antibioticExposure);
        assertFalse(compiled.orderDetail.criticalNotificationPreference);
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
