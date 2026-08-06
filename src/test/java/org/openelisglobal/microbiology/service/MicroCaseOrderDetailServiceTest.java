package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private MicroCaseOrderDetailService service;

    @Before
    public void setUp() {
        service = new MicroCaseOrderDetailServiceImpl(orderDetailDAO, caseDAO, activityDAO);
    }

    @Test
    public void saveOrderDetailCreatesRecordWhenNoneExists() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(orderDetailDAO.getByCaseId("case-1")).thenReturn(null);
        MicroCaseOrderDetailRequestForm request = new MicroCaseOrderDetailRequestForm();
        request.patientOrigin = "Inpatient ward 3";
        request.numberOfSets = 2;
        request.clinicalHistory = "Fever, suspected sepsis";
        request.antibioticExposure = true;
        request.criticalNotificationPreference = true;

        MicroCaseOrderDetail saved = service.saveOrderDetail("case-1", request, "1");

        assertEquals("case-1", saved.getCaseId());
        assertEquals("Inpatient ward 3", saved.getPatientOrigin());
        assertEquals(Integer.valueOf(2), saved.getNumberOfSets());
        assertEquals("Fever, suspected sepsis", saved.getClinicalHistory());
        assertEquals(Boolean.TRUE, saved.getAntibioticExposure());
        assertEquals(Boolean.TRUE, saved.getCriticalNotificationPreference());
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
        existing.setPatientOrigin("Emergency department");
        when(orderDetailDAO.getByCaseId("case-1")).thenReturn(existing);
        MicroCaseOrderDetailRequestForm request = new MicroCaseOrderDetailRequestForm();
        request.patientOrigin = "Inpatient ward 3";
        request.numberOfSets = 3;

        MicroCaseOrderDetail saved = service.saveOrderDetail("case-1", request, "2");

        assertEquals("detail-1", saved.getId());
        assertEquals("Inpatient ward 3", saved.getPatientOrigin());
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
        request.patientOrigin = "INPATIENT";
        request.numberOfSets = 2;
        request.clinicalHistory = "Persistent fever";
        request.antibioticExposure = true;
        request.criticalNotificationPreference = false;

        MicroCaseOrderDetail saved = service.saveOrderDraft(sample, request, "7");

        assertNull(saved.getCaseId());
        assertEquals("99", saved.getSampleId());
        assertEquals("17", saved.getCultureMethodId());
        assertEquals(Boolean.TRUE, saved.getAntibioticExposure());
        assertEquals(Boolean.FALSE, saved.getCriticalNotificationPreference());
        verify(orderDetailDAO).insert(saved);
        verify(activityDAO, never()).insert(any(MicroCaseActivity.class));
    }

    @Test
    public void getOrderDraftCompilesTheReloadForm() {
        MicroCaseOrderDetail detail = new MicroCaseOrderDetail();
        detail.setSampleId("99");
        detail.setCultureMethodId("17");
        detail.setPatientOrigin("INPATIENT");
        detail.setNumberOfSets(3);
        detail.setClinicalHistory("Sepsis query");
        detail.setAntibioticExposure(false);
        detail.setCriticalNotificationPreference(true);
        when(orderDetailDAO.getDraftBySampleId("99")).thenReturn(detail);

        MicroCaseOrderDetailRequestForm reloaded = service.getOrderDraft("99");

        assertEquals("17", reloaded.cultureMethodId);
        assertEquals("INPATIENT", reloaded.patientOrigin);
        assertEquals(Integer.valueOf(3), reloaded.numberOfSets);
        assertEquals("Sepsis query", reloaded.clinicalHistory);
        assertEquals(Boolean.FALSE, reloaded.antibioticExposure);
        assertEquals(Boolean.TRUE, reloaded.criticalNotificationPreference);
    }
}
