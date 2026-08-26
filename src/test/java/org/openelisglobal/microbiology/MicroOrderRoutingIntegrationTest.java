package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.valueholder.Analysis;
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

    private String methodId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        methodId = fixtures.createMethodId();
        fixtures.createReferenceData(methodId);
        fixtures.createTbCultureSetup(methodId);
    }

    @Test
    public void routesNonMicroBacteriologyAndSiblingWorkflowCases() {
        SampleItem sampleItem = fixtures.createSampleWithSampleItem("OGC782M3");
        org.openelisglobal.test.valueholder.Test nonMicroTest = fixtures.createCatalogTest();
        org.openelisglobal.test.valueholder.Test bacteriologyTest = fixtures.createCatalogCultureTest(methodId,
                MicroWorkflowType.BACTERIOLOGY);
        org.openelisglobal.test.valueholder.Test tbTest = fixtures.createCatalogCultureTest(methodId,
                MicroWorkflowType.MYCOBACTERIOLOGY_TB);

        routingService.routeAnalysesForSampleItem(sampleItem, List.of(analysis(nonMicroTest)),
                fixtures.defaultUserId());
        assertEquals(0, caseService.getSiblingCases(sampleItem.getId()).size());

        routingService.routeAnalysesForSampleItem(sampleItem, List.of(analysis(bacteriologyTest), analysis(tbTest)),
                fixtures.defaultUserId());

        assertEquals(2, caseService.getSiblingCases(sampleItem.getId()).size());
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
        orderDetail.admissionDate = "2026-08-03";
        orderDetail.numberOfSets = 2;
        orderDetail.clinicalHistory = "Fever and hypotension";
        orderDetail.antibioticExposure = true;

        List<MicroCase> first = routingService.routeAnalysesForSampleItem(persistedItem, List.of(persistedAnalysis),
                fixtures.defaultUserId(), orderDetail);
        List<MicroCase> repeated = routingService.routeAnalysesForSampleItem(persistedItem, List.of(persistedAnalysis),
                fixtures.defaultUserId(), orderDetail);

        assertEquals(1, first.size());
        assertEquals(first.get(0).getId(), repeated.get(0).getId());
        assertEquals(1, caseService.getSiblingCases(persistedItem.getId()).size());
        MicroCaseOrderDetail persisted = orderDetailService.getOrderDetail(first.get(0).getId());
        assertEquals(Integer.valueOf(2), persisted.getNumberOfSets());
        assertEquals(LocalDate.of(2026, 8, 3), persisted.getAdmissionDate());
        assertTrue(persisted.getAntibioticExposure());

        MicroCaseDetailForm compiled = caseService.getCaseDetail(first.get(0).getId());
        assertEquals("EMERGENCY", compiled.orderDetail.patientOrigin);
        assertEquals("2026-08-03", compiled.orderDetail.admissionDate);
        assertEquals("Fever and hypotension", compiled.orderDetail.clinicalHistory);
        assertTrue(compiled.orderDetail.antibioticExposure);
    }

    @Test
    public void orderEntryDraftSurvivesSearchReloadAndRoutesIntoTheCase() {
        SampleItem persistedItem = fixtures.createSampleWithSampleItem("OGC782M3R");
        org.openelisglobal.test.valueholder.Test cultureTest = fixtures.createCatalogCultureTest(methodId,
                MicroWorkflowType.BACTERIOLOGY);
        Analysis persistedAnalysis = fixtures.createAnalysis(persistedItem, cultureTest);
        MicroCaseOrderDetailRequestForm orderDetail = new MicroCaseOrderDetailRequestForm();
        orderDetail.cultureMethodId = methodId;
        orderDetail.patientOrigin = "INPATIENT";
        orderDetail.admissionDate = "2026-08-13";
        orderDetail.numberOfSets = 3;
        orderDetail.clinicalHistory = "Persistent fever after antibiotics";
        orderDetail.antibioticExposure = true;
        orderDetailService.saveOrderDraft(persistedItem.getSample(), orderDetail, fixtures.defaultUserId());

        MicroCaseOrderDetailRequestForm reloaded = orderDetailService.getOrderDraft(persistedItem.getSample().getId());
        assertEquals(methodId, reloaded.cultureMethodId);
        assertEquals("Persistent fever after antibiotics", reloaded.clinicalHistory);
        assertEquals("2026-08-13", reloaded.admissionDate);

        List<MicroCase> routed = routingService.routeAnalysesForSampleItem(persistedItem, List.of(persistedAnalysis),
                fixtures.defaultUserId(), null);

        assertEquals(1, routed.size());
        MicroCaseOrderDetail caseDetail = orderDetailService.getOrderDetail(routed.get(0).getId());
        assertEquals("INPATIENT", caseDetail.getPatientOrigin());
        assertEquals(LocalDate.of(2026, 8, 13), caseDetail.getAdmissionDate());
        assertEquals(Integer.valueOf(3), caseDetail.getNumberOfSets());
        assertTrue(caseDetail.getAntibioticExposure());
    }

    private Analysis analysis(org.openelisglobal.test.valueholder.Test test) {
        Analysis analysis = new Analysis();
        analysis.setTest(test);
        return analysis;
    }
}
