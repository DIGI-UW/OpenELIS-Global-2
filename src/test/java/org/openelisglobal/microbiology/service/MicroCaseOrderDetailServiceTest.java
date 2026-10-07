package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseOrderDetailDAO;
import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCaseOrderDetail;

@RunWith(MockitoJUnitRunner.class)
public class MicroCaseOrderDetailServiceTest {

    @Mock
    private MicroCaseOrderDetailDAO orderDetailDAO;

    @Mock
    private MicroCaseDAO caseDAO;

    @Mock
    private MicroCaseActivityDAO activityDAO;

    @Mock
    private MicrobiologyReferenceService referenceService;

    @Mock
    private MicrobiologyCaseAccessService accessService;

    private MicroCaseOrderDetailService service;

    @Before
    public void setUp() {
        when(referenceService.isActivePatientOriginCode(org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
        service = new MicroCaseOrderDetailServiceImpl(orderDetailDAO, caseDAO, activityDAO, referenceService,
                new ObjectMapper(), accessService);
    }

    @Test
    public void saveOrderDetailCreatesRecordWhenNoneExists() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(orderDetailDAO.getByCaseId("case-1")).thenReturn(null);
        MicroCaseOrderDetailRequestForm request = new MicroCaseOrderDetailRequestForm();
        request.culturePurpose = "CLINICAL_DIAGNOSTIC";
        request.patientOrigin = "INPATIENT";
        request.admissionDate = "2026-08-03";
        request.clinicalHistory = "Fever, suspected sepsis";
        request.antibioticExposure = true;

        MicroCaseOrderDetail saved = service.saveOrderDetail("case-1", request, "1");

        assertEquals("case-1", saved.getCaseId());
        assertEquals("CLINICAL_DIAGNOSTIC", saved.getCulturePurpose());
        assertEquals("INPATIENT", saved.getPatientOrigin());
        assertEquals(LocalDate.of(2026, 8, 3), saved.getAdmissionDate());
        assertEquals(null, saved.getNumberOfSets());
        assertEquals("Fever, suspected sepsis", saved.getClinicalHistory());
        assertEquals(Boolean.TRUE, saved.getAntibioticExposure());
        assertNotNull(saved.getCreatedAt());
        verify(orderDetailDAO).insert(saved);
        verify(orderDetailDAO, never()).update(any(MicroCaseOrderDetail.class));
        ArgumentCaptor<MicroCaseActivity> activity = ArgumentCaptor.forClass(MicroCaseActivity.class);
        verify(activityDAO).insert(activity.capture());
        assertEquals(MicroCaseActivityType.ORDER_DETAIL_CAPTURED.name(), activity.getValue().getActivityType());
    }

    @Test
    public void saveOrderDetailUpdatesExistingRecordInPlace() throws Exception {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        MicroCaseOrderDetail existing = new MicroCaseOrderDetail();
        existing.setId("detail-1");
        existing.setCaseId("case-1");
        existing.setPatientOrigin("EMERGENCY");
        existing.setNumberOfSets(2);
        when(orderDetailDAO.getByCaseId("case-1")).thenReturn(existing);
        MicroCaseOrderDetailRequestForm request = new ObjectMapper().readValue("{\"numberOfSets\":77}",
                MicroCaseOrderDetailRequestForm.class);
        request.patientOrigin = "INPATIENT";
        request.culturePurpose = "ACTIVE_SCREENING";
        request.admissionDate = "2026-08-03";

        MicroCaseOrderDetail saved = service.saveOrderDetail("case-1", request, "2");

        assertEquals("detail-1", saved.getId());
        assertEquals("INPATIENT", saved.getPatientOrigin());
        assertEquals("ACTIVE_SCREENING", saved.getCulturePurpose());
        assertEquals(Integer.valueOf(2), saved.getNumberOfSets());
        verify(orderDetailDAO).update(existing);
        verify(orderDetailDAO, never()).insert(any(MicroCaseOrderDetail.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void saveOrderDetailRejectsUnknownCase() {
        when(caseDAO.get("missing")).thenReturn(Optional.empty());

        service.saveOrderDetail("missing", new MicroCaseOrderDetailRequestForm(), "1");
    }

    @Test
    public void getOrderDetailReturnsNullWhenNoneCaptured() {
        when(orderDetailDAO.getByCaseId("case-1")).thenReturn(null);

        assertEquals(null, service.getOrderDetail("case-1"));
    }

    @Test
    public void saveOrderDetailAuditsAPreReleaseCulturePurposeCorrection() throws Exception {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setStage("INCUBATING");
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        MicroCaseOrderDetail existing = new MicroCaseOrderDetail();
        existing.setId("detail-1");
        existing.setCaseId("case-1");
        existing.setCulturePurpose("CLINICAL_DIAGNOSTIC");
        when(orderDetailDAO.getByCaseId("case-1")).thenReturn(existing);
        MicroCaseOrderDetailRequestForm request = new ObjectMapper().readValue("{\"numberOfSets\":77}",
                MicroCaseOrderDetailRequestForm.class);
        request.culturePurpose = "ACTIVE_SCREENING";

        service.saveOrderDetail("case-1", request, "2");

        ArgumentCaptor<MicroCaseActivity> activity = ArgumentCaptor.forClass(MicroCaseActivity.class);
        verify(activityDAO).insert(activity.capture());
        assertEquals(MicroCaseActivityType.CULTURE_PURPOSE_CHANGED.name(), activity.getValue().getActivityType());
        JsonNode audit = new ObjectMapper().readTree(activity.getValue().getStructuredData());
        assertEquals("CLINICAL_DIAGNOSTIC", audit.get("fromPurpose").asText());
        assertEquals("ACTIVE_SCREENING", audit.get("toPurpose").asText());
        assertEquals("2", activity.getValue().getPerformedBy());
    }

    @Test
    public void saveOrderDetailRejectsCulturePurposeCorrectionAfterFinalRelease() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setStage("FINAL_RELEASED");
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        MicroCaseOrderDetail existing = new MicroCaseOrderDetail();
        existing.setCaseId("case-1");
        existing.setCulturePurpose("CLINICAL_DIAGNOSTIC");
        MicroCaseOrderDetailRequestForm request = new MicroCaseOrderDetailRequestForm();
        request.culturePurpose = "ACTIVE_SCREENING";

        assertThrows(MicroCaseLockedException.class, () -> service.saveOrderDetail("case-1", request, "2"));

        verify(orderDetailDAO, never()).update(existing);
    }

    @Test
    public void inpatientAdmissionDateIsOptionalAtCaseEntry() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        MicroCaseOrderDetailRequestForm request = new MicroCaseOrderDetailRequestForm();
        request.patientOrigin = "INPATIENT";

        MicroCaseOrderDetail saved = service.saveOrderDetail("case-1", request, "7");

        assertNull(saved.getAdmissionDate());
        assertEquals("7", saved.getCreatedBy());
    }

    @Test
    public void outpatientCaseDetailsClearAdmissionDate() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        MicroCaseOrderDetailRequestForm request = new MicroCaseOrderDetailRequestForm();
        request.patientOrigin = "OUTPATIENT";
        request.admissionDate = "2026-08-03";

        assertNull(service.saveOrderDetail("case-1", request, "7").getAdmissionDate());
    }

    @Test
    public void malformedAdmissionDateDoesNotPersistCaseDetails() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        MicroCaseOrderDetailRequestForm request = new MicroCaseOrderDetailRequestForm();
        request.patientOrigin = "INPATIENT";
        request.admissionDate = "2026-02-31";

        assertThrows(IllegalArgumentException.class, () -> service.saveOrderDetail("case-1", request, "7"));

        verify(orderDetailDAO, never()).insert(any(MicroCaseOrderDetail.class));
        verify(activityDAO, never()).insert(any(MicroCaseActivity.class));
    }

    @Test
    public void inactivePatientOriginDoesNotPersistCaseDetails() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(referenceService.isActivePatientOriginCode("RETIRED")).thenReturn(false);
        MicroCaseOrderDetailRequestForm request = new MicroCaseOrderDetailRequestForm();
        request.patientOrigin = "RETIRED";

        assertThrows(IllegalArgumentException.class, () -> service.saveOrderDetail("case-1", request, "7"));

        verify(orderDetailDAO, never()).insert(any(MicroCaseOrderDetail.class));
    }

    @Test
    public void caseInformationWriteRequiresResultsInTheCaseUnit() {
        org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException("unit rights"))
                .when(accessService).requireResults("case-1", "17");

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.saveOrderDetail("case-1", new MicroCaseOrderDetailRequestForm(), "17"));

        verify(orderDetailDAO, never()).insert(any(MicroCaseOrderDetail.class));
        verify(orderDetailDAO, never()).update(any(MicroCaseOrderDetail.class));
        verify(activityDAO, never()).insert(any(MicroCaseActivity.class));
    }
}
