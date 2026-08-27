package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseOrderDetailDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.form.MicroCaseDetailForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCaseOrderDetail;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.qaevent.service.NceSpecimenService;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

@RunWith(MockitoJUnitRunner.class)
public class MicroCaseServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;

    @Mock
    private MicroCaseActivityDAO activityDAO;

    @Mock
    private MicroIsolateDAO isolateDAO;

    @Mock
    private MicroCaseOrderDetailDAO orderDetailDAO;

    @Mock
    private SampleItemService sampleItemService;

    @Mock
    private SampleHumanService sampleHumanService;

    @Mock
    private PatientService patientService;

    @Mock
    private SampleOrganizationService sampleOrganizationService;

    @Mock
    private SystemUserService systemUserService;

    @Mock
    private NceSpecimenService nceSpecimenService;

    @Test
    public void createOrGetCaseReturnsExistingCaseWithoutDuplicateActivity() {
        MicroCase existing = new MicroCase();
        existing.setSampleItemId("1001");
        existing.setWorkflowType(MicroWorkflowType.BACTERIOLOGY.name());
        when(caseDAO.getBySampleItemAndWorkflow("1001", MicroWorkflowType.BACTERIOLOGY.name())).thenReturn(existing);

        MicroCaseService service = service();
        MicroCase result = service.createOrGetCase("1001", MicroWorkflowType.BACTERIOLOGY, "1", "1");

        assertEquals(existing, result);
        verify(caseDAO, never()).insert(any(MicroCase.class));
        verify(activityDAO, never()).insert(any(MicroCaseActivity.class));
    }

    @Test
    public void createOrGetCaseCreatesReceivedCaseAndTimelineActivity() {
        MicroCaseService service = service();

        MicroCase result = service.createOrGetCase("1001", MicroWorkflowType.BACTERIOLOGY, "1", "1");

        assertEquals("1001", result.getSampleItemId());
        assertEquals(MicroWorkflowType.BACTERIOLOGY.name(), result.getWorkflowType());
        assertEquals(MicroCaseStage.RECEIVED.name(), result.getStage());
        assertNotNull(result.getCreatedAt());
        verify(caseDAO).insert(result);
        ArgumentCaptor<MicroCaseActivity> activityCaptor = ArgumentCaptor.forClass(MicroCaseActivity.class);
        verify(activityDAO).insert(activityCaptor.capture());
        assertEquals(result.getId(), activityCaptor.getValue().getCaseId());
        assertEquals(MicroCaseActivityType.CASE_CREATED.name(), activityCaptor.getValue().getActivityType());
    }

    @Test
    public void getCaseDetailCompilesOrderDetailWhenCaptured() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(activityDAO.getByCaseId("case-1")).thenReturn(java.util.List.of());
        when(isolateDAO.getByCaseId("case-1")).thenReturn(java.util.List.of());
        MicroCaseOrderDetail detail = new MicroCaseOrderDetail();
        detail.setCaseId("case-1");
        detail.setPatientOrigin("Emergency department");
        detail.setNumberOfSets(2);
        when(orderDetailDAO.getByCaseId("case-1")).thenReturn(detail);

        MicroCaseService service = service();
        MicroCaseDetailForm form = service.getCaseDetail("case-1");

        assertNotNull(form.orderDetail);
        assertEquals("Emergency department", form.orderDetail.patientOrigin);
        assertEquals(Integer.valueOf(2), form.orderDetail.numberOfSets);
    }

    @Test
    public void getCaseDetailLeavesOrderDetailNullWhenNotCaptured() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(activityDAO.getByCaseId("case-1")).thenReturn(java.util.List.of());
        when(isolateDAO.getByCaseId("case-1")).thenReturn(java.util.List.of());
        when(orderDetailDAO.getByCaseId("case-1")).thenReturn(null);

        MicroCaseService service = service();
        MicroCaseDetailForm form = service.getCaseDetail("case-1");

        assertEquals(null, form.orderDetail);
    }

    @Test
    public void getCaseDetailCompilesPatientAccessionAndSpecimenContext() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setSampleItemId("1001");
        Sample sample = new Sample();
        sample.setAccessionNumber("UATMICRO001");
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("1001");
        sampleItem.setSample(sample);
        TypeOfSample typeOfSample = new TypeOfSample();
        typeOfSample.setDescription("Blood");
        sampleItem.setTypeOfSample(typeOfSample);
        Patient patient = new Patient();
        patient.setId("patient-1");
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(activityDAO.getByCaseId("case-1")).thenReturn(java.util.List.of());
        when(isolateDAO.getByCaseId("case-1")).thenReturn(java.util.List.of());
        when(sampleItemService.getData("1001")).thenReturn(sampleItem);
        when(sampleHumanService.getPatientForSample(sample)).thenReturn(patient);
        when(patientService.getLastFirstName(patient)).thenReturn("Microbiology, UAT");
        SampleOrganization sampleOrganization = new SampleOrganization();
        Organization organization = new Organization();
        organization.setOrganizationName("Medical ward 2");
        sampleOrganization.setOrganization(organization);
        when(sampleOrganizationService.getDataBySample(sample)).thenReturn(sampleOrganization);
        when(nceSpecimenService.getSpecimenBySampleItemId(1001))
                .thenReturn(java.util.List.of(new NceSpecimen(), new NceSpecimen()));

        MicroCaseDetailForm form = service().getCaseDetail("case-1");

        assertEquals("patient-1", form.patientId);
        assertEquals("Microbiology, UAT", form.patientName);
        assertEquals("UATMICRO001", form.accessionNumber);
        assertEquals("Blood", form.specimenType);
        assertEquals("Medical ward 2", form.requestingLocation);
        assertEquals(2, form.nonconformanceCount);
    }

    @Test
    public void getCaseDetailResolvesLastActivityActorForDisplay() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        MicroCaseActivity first = new MicroCaseActivity();
        first.setId("1");
        first.setCaseId("case-1");
        first.setPerformedBy("7");
        first.setOccurredAt(java.sql.Timestamp.valueOf("2026-08-05 08:00:00"));
        MicroCaseActivity latest = new MicroCaseActivity();
        latest.setId("2");
        latest.setCaseId("case-1");
        latest.setPerformedBy("8");
        latest.setOccurredAt(java.sql.Timestamp.valueOf("2026-08-05 09:00:00"));
        SystemUser user = new SystemUser();
        user.setFirstName("Amina");
        user.setLastName("Diallo");
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(activityDAO.getByCaseId("case-1")).thenReturn(java.util.List.of(first, latest));
        when(isolateDAO.getByCaseId("case-1")).thenReturn(java.util.List.of());
        when(systemUserService.getUserById("7")).thenReturn(null);
        when(systemUserService.getUserById("8")).thenReturn(user);

        MicroCaseDetailForm form = service().getCaseDetail("case-1");

        assertEquals("Amina Diallo", form.activities.get(1).performedByDisplay);
        assertEquals("Amina Diallo", form.lastActivityBy);
        assertEquals(latest.getOccurredAt(), form.lastActivityAt);
    }

    @Test
    public void getCaseDetailIncludesSiblingWorkflowLinksAndPreservationWarning() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setSampleItemId("1001");
        microCase.setWorkflowType(MicroWorkflowType.BACTERIOLOGY.name());
        microCase.setStage(MicroCaseStage.SETUP_RECORDED.name());
        MicroCase sibling = new MicroCase();
        sibling.setId("case-2");
        sibling.setSampleItemId("1001");
        sibling.setWorkflowType(MicroWorkflowType.MYCOBACTERIOLOGY_TB.name());
        sibling.setStage(MicroCaseStage.RECEIVED.name());
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(activityDAO.getByCaseId("case-1")).thenReturn(java.util.List.of());
        when(isolateDAO.getByCaseId("case-1")).thenReturn(java.util.List.of());
        when(caseDAO.getBySampleItem("1001")).thenReturn(java.util.List.of(microCase, sibling));

        MicroCaseDetailForm form = service().getCaseDetail("case-1");

        assertTrue(form.workflowChangeRequiresConfirmation);
        assertEquals(1, form.siblingCases.size());
        assertEquals("case-2", form.siblingCases.get(0).id);
        assertEquals(MicroWorkflowType.MYCOBACTERIOLOGY_TB.name(), form.siblingCases.get(0).workflowType);
    }

    private MicroCaseService service() {
        return new MicroCaseServiceImpl(caseDAO, activityDAO, isolateDAO, orderDetailDAO, sampleItemService,
                sampleHumanService, patientService, sampleOrganizationService, systemUserService, nceSpecimenService);
    }
}
