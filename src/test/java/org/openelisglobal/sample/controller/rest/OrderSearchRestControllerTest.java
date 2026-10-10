package org.openelisglobal.sample.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.sample.service.OrderProgressService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.dto.TestSelectionDTO;
import org.openelisglobal.testmethod.service.TestMethodService;
import org.openelisglobal.testmethod.service.TestMethodService.TestMethodDto;
import org.springframework.test.util.ReflectionTestUtils;

public class OrderSearchRestControllerTest {

    @Test
    public void mapsCanonicalProgramIdentityForReloadedOrders() {
        OrderSearchRestController controller = new OrderSearchRestController();
        Program program = new Program();
        program.setId("8");
        program.setProgramName("Microbiology");
        program.setCode("MICROBIOLOGY");
        Map<String, Object> sampleOrderItems = new HashMap<>();

        ReflectionTestUtils.invokeMethod(controller, "addProgramSelection", sampleOrderItems, program);

        assertEquals("8", sampleOrderItems.get("programId"));
        assertEquals("Microbiology", sampleOrderItems.get("program"));
        assertEquals("MICROBIOLOGY", sampleOrderItems.get("programCode"));
    }

    @Test
    public void mapsMethodsForReloadedOrders() {
        OrderSearchRestController controller = new OrderSearchRestController();
        TestMethodService testMethodService = mock(TestMethodService.class);
        org.openelisglobal.test.valueholder.Test test = mock(org.openelisglobal.test.valueholder.Test.class);
        when(test.getId()).thenReturn("42");
        when(test.getLocalizedName()).thenReturn("Blood culture");
        when(test.getDescription()).thenReturn("Blood culture");
        TestMethodDto defaultMethod = new TestMethodDto();
        defaultMethod.methodId = "7";
        defaultMethod.methodName = "Blood Culture Standard";
        defaultMethod.isDefault = true;
        when(testMethodService.getLinkedMethodDtos("42")).thenReturn(List.of(defaultMethod));
        ReflectionTestUtils.setField(controller, "testMethodService", testMethodService);

        TestSelectionDTO selectedTest = controller.buildSelectedTestData(test);

        assertEquals("42", selectedTest.getId());
        assertSame(defaultMethod, selectedTest.getMethods().get(0));
    }

    // ── OGC-1423: the dashboard's referral counts and its Has referred tests
    // filter ──

    private static Analysis analysis(String id, String statusId) {
        Analysis analysis = new Analysis();
        analysis.setId(id);
        analysis.setStatusId(statusId);
        return analysis;
    }

    private static Referral referral(String id, String laboratory) {
        Referral referral = new Referral();
        referral.setId(id);
        Organization organization = new Organization();
        organization.setOrganizationName(laboratory);
        referral.setOrganization(organization);
        return referral;
    }

    private static OrderSearchRestController referralAwareController(AnalysisService analysisService,
            ReferralService referralService, IStatusService statusService, OrderProgressService orderProgressService) {
        OrderSearchRestController controller = new OrderSearchRestController();
        ReflectionTestUtils.setField(controller, "analysisService", analysisService);
        ReflectionTestUtils.setField(controller, "referralService", referralService);
        ReflectionTestUtils.setField(controller, "statusService", statusService);
        ReflectionTestUtils.setField(controller, "orderProgressService", orderProgressService);
        return controller;
    }

    @Test
    public void referralSummaryCountsOnlyOpenReferralsOnActiveTests() {
        AnalysisService analysisService = mock(AnalysisService.class);
        ReferralService referralService = mock(ReferralService.class);
        IStatusService statusService = mock(IStatusService.class);
        OrderProgressService orderProgressService = mock(OrderProgressService.class);
        OrderSearchRestController controller = referralAwareController(analysisService, referralService, statusService,
                orderProgressService);

        SampleItem tube = new SampleItem();
        tube.setId("10");
        Analysis referred = analysis("1", "4");
        Analysis rejectedByLab = analysis("2", "4");
        Analysis inHouse = analysis("3", "4");
        Analysis cancelledTest = analysis("4", "9");
        when(analysisService.getAnalysesBySampleItem(tube))
                .thenReturn(List.of(referred, rejectedByLab, inHouse, cancelledTest));
        when(statusService.matches("4", AnalysisStatus.Canceled)).thenReturn(false);
        when(statusService.matches("9", AnalysisStatus.Canceled)).thenReturn(true);
        Referral open = referral("r1", "CEDRES");
        Referral rejected = referral("r2", "Institut Pasteur");
        Referral onCancelledTest = referral("r4", "Elsewhere");
        when(referralService.getReferralByAnalysisId("1")).thenReturn(open);
        when(referralService.getReferralByAnalysisId("2")).thenReturn(rejected);
        when(referralService.getReferralByAnalysisId("3")).thenReturn(null);
        when(referralService.getReferralByAnalysisId("4")).thenReturn(onCancelledTest);
        when(orderProgressService.isOpenReferral(open)).thenReturn(true);
        when(orderProgressService.isOpenReferral(rejected)).thenReturn(false);
        when(orderProgressService.isOpenReferral(onCancelledTest)).thenReturn(true);

        Map<String, Object> summary = controller.referralSummary(List.of(tube));

        assertEquals("a cancelled test is not a test of the order", 3, summary.get("totalTests"));
        assertEquals("the rejected referral is back in-house", 1, summary.get("referredTests"));
        assertEquals("CEDRES", summary.get("referredTo"));
    }

    @Test
    public void hasReferredTestsMeansAnOpenReferralOnAnActiveTest() {
        AnalysisService analysisService = mock(AnalysisService.class);
        ReferralService referralService = mock(ReferralService.class);
        IStatusService statusService = mock(IStatusService.class);
        OrderProgressService orderProgressService = mock(OrderProgressService.class);
        OrderSearchRestController controller = referralAwareController(analysisService, referralService, statusService,
                orderProgressService);

        SampleItem tube = new SampleItem();
        tube.setId("10");
        Analysis first = analysis("1", "4");
        Analysis cancelledTest = analysis("2", "9");
        when(analysisService.getAnalysesBySampleItem(tube)).thenReturn(List.of(first, cancelledTest));
        when(statusService.matches("4", AnalysisStatus.Canceled)).thenReturn(false);
        when(statusService.matches("9", AnalysisStatus.Canceled)).thenReturn(true);
        Referral rejected = referral("r1", "CEDRES");
        Referral onCancelledTest = referral("r2", "CEDRES");
        when(referralService.getReferralByAnalysisId("1")).thenReturn(rejected);
        when(referralService.getReferralByAnalysisId("2")).thenReturn(onCancelledTest);
        when(orderProgressService.isOpenReferral(rejected)).thenReturn(false);
        when(orderProgressService.isOpenReferral(onCancelledTest)).thenReturn(true);

        assertFalse("only a rejected referral and a cancelled test: nothing is referred",
                controller.hasReferral(List.of(tube)));

        Referral open = referral("r3", "CEDRES");
        when(referralService.getReferralByAnalysisId("1")).thenReturn(open);
        when(orderProgressService.isOpenReferral(open)).thenReturn(true);
        assertTrue(controller.hasReferral(List.of(tube)));
    }
}
