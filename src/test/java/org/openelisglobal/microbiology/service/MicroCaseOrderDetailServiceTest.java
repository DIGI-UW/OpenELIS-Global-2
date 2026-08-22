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
import org.openelisglobal.sample.valueholder.Sample;

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

    private MicroCaseOrderDetailService service;

    @Before
    public void setUp() {
        when(referenceService.isActivePatientOriginCode(org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
        service = new MicroCaseOrderDetailServiceImpl(orderDetailDAO, caseDAO, activityDAO, referenceService,
                new ObjectMapper());
    }

    @Test
    public void saveOrderDetailCreatesRecordWhenNoneExists() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setCultureMethodId("configured-method");
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(orderDetailDAO.getByCaseId("case-1")).thenReturn(null);
        MicroCaseOrderDetailRequestForm request = new MicroCaseOrderDetailRequestForm();
        request.cultureMethodId = "submitted-method";
        request.culturePurpose = "CLINICAL_DIAGNOSTIC";
        request.patientOrigin = "INPATIENT";
        request.admissionDate = "2026-08-03";
        request.numberOfSets = 2;
        request.clinicalHistory = "Fever, suspected sepsis";
        request.antibioticExposure = true;

        MicroCaseOrderDetail saved = service.saveOrderDetail("case-1", request, "1");

        assertEquals("case-1", saved.getCaseId());
        assertEquals("configured-method", saved.getCultureMethodId());
        assertEquals("CLINICAL_DIAGNOSTIC", saved.getCulturePurpose());
        assertEquals("INPATIENT", saved.getPatientOrigin());
        assertEquals(LocalDate.of(2026, 8, 3), saved.getAdmissionDate());
        assertEquals(Integer.valueOf(2), saved.getNumberOfSets());
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
    public void saveOrderDetailUpdatesExistingRecordInPlace() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        MicroCaseOrderDetail existing = new MicroCaseOrderDetail();
        existing.setId("detail-1");
        existing.setCaseId("case-1");
        existing.setPatientOrigin("EMERGENCY");
        when(orderDetailDAO.getByCaseId("case-1")).thenReturn(existing);
        MicroCaseOrderDetailRequestForm request = new MicroCaseOrderDetailRequestForm();
        request.patientOrigin = "INPATIENT";
        request.culturePurpose = "ACTIVE_SCREENING";
        request.admissionDate = "2026-08-03";
        request.numberOfSets = 3;

        MicroCaseOrderDetail saved = service.saveOrderDetail("case-1", request, "2");

        assertEquals("detail-1", saved.getId());
        assertEquals("INPATIENT", saved.getPatientOrigin());
        assertEquals("ACTIVE_SCREENING", saved.getCulturePurpose());
        assertEquals(Integer.valueOf(3), saved.getNumberOfSets());
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
    public void saveOrderDraftPersistsPreCaseDetailBySample() {
        when(orderDetailDAO.getDraftBySampleId("99")).thenReturn(null);
        Sample sample = new Sample();
        sample.setId("99");
        MicroCaseOrderDetailRequestForm request = new MicroCaseOrderDetailRequestForm();
        request.cultureMethodId = "17";
        request.culturePurpose = "CLINICAL_DIAGNOSTIC";
        request.patientOrigin = "INPATIENT";
        request.admissionDate = "2026-08-03";
        request.numberOfSets = 2;
        request.clinicalHistory = "Persistent fever";
        request.antibioticExposure = true;

        MicroCaseOrderDetail saved = service.saveOrderDraft(sample, request, "7");

        assertNull(saved.getCaseId());
        assertEquals("99", saved.getSampleId());
        assertEquals("17", saved.getCultureMethodId());
        assertEquals("CLINICAL_DIAGNOSTIC", saved.getCulturePurpose());
        assertEquals(LocalDate.of(2026, 8, 3), saved.getAdmissionDate());
        assertEquals(Boolean.TRUE, saved.getAntibioticExposure());
        verify(orderDetailDAO).insert(saved);
        verify(activityDAO, never()).insert(any(MicroCaseActivity.class));
    }

    @Test
    public void getOrderDraftCompilesTheReloadForm() {
        MicroCaseOrderDetail detail = new MicroCaseOrderDetail();
        detail.setSampleId("99");
        detail.setCultureMethodId("17");
        detail.setCulturePurpose("ACTIVE_SCREENING");
        detail.setPatientOrigin("INPATIENT");
        detail.setAdmissionDate(LocalDate.of(2026, 8, 13));
        detail.setNumberOfSets(3);
        detail.setClinicalHistory("Sepsis query");
        detail.setAntibioticExposure(false);
        when(orderDetailDAO.getDraftBySampleId("99")).thenReturn(detail);

        MicroCaseOrderDetailRequestForm reloaded = service.getOrderDraft("99");

        assertEquals("17", reloaded.cultureMethodId);
        assertEquals("ACTIVE_SCREENING", reloaded.culturePurpose);
        assertEquals("INPATIENT", reloaded.patientOrigin);
        assertEquals("2026-08-13", reloaded.admissionDate);
        assertEquals(Integer.valueOf(3), reloaded.numberOfSets);
        assertEquals("Sepsis query", reloaded.clinicalHistory);
        assertEquals(Boolean.FALSE, reloaded.antibioticExposure);
    }

    @Test
    public void saveOrderDraftClearsAdmissionDateForOutpatientContext() {
        when(orderDetailDAO.getDraftBySampleId("99")).thenReturn(null);
        Sample sample = new Sample();
        sample.setId("99");
        MicroCaseOrderDetailRequestForm request = new MicroCaseOrderDetailRequestForm();
        request.culturePurpose = "CLINICAL_DIAGNOSTIC";
        request.patientOrigin = "OUTPATIENT";
        request.admissionDate = "2026-08-03";

        MicroCaseOrderDetail saved = service.saveOrderDraft(sample, request, "7");

        assertNull(saved.getAdmissionDate());
    }

    @Test(expected = IllegalArgumentException.class)
    public void saveOrderDraftRejectsMalformedAdmissionDate() {
        when(orderDetailDAO.getDraftBySampleId("99")).thenReturn(null);
        Sample sample = new Sample();
        sample.setId("99");
        MicroCaseOrderDetailRequestForm request = new MicroCaseOrderDetailRequestForm();
        request.culturePurpose = "CLINICAL_DIAGNOSTIC";
        request.patientOrigin = "INPATIENT";
        request.admissionDate = "2026-02-31";

        service.saveOrderDraft(sample, request, "7");
    }

    @Test(expected = IllegalArgumentException.class)
    public void saveOrderDraftRejectsUnknownPatientOriginCode() {
        when(referenceService.isActivePatientOriginCode("FREE_TEXT")).thenReturn(false);
        Sample sample = new Sample();
        sample.setId("99");
        MicroCaseOrderDetailRequestForm request = new MicroCaseOrderDetailRequestForm();
        request.culturePurpose = "CLINICAL_DIAGNOSTIC";
        request.patientOrigin = "FREE_TEXT";

        service.saveOrderDraft(sample, request, "7");
    }

    @Test(expected = IllegalArgumentException.class)
    public void saveOrderDraftRejectsMissingCulturePurposeForANewOrder() {
        when(orderDetailDAO.getDraftBySampleId("99")).thenReturn(null);
        Sample sample = new Sample();
        sample.setId("99");

        service.saveOrderDraft(sample, new MicroCaseOrderDetailRequestForm(), "7");
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
        MicroCaseOrderDetailRequestForm request = new MicroCaseOrderDetailRequestForm();
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
}
