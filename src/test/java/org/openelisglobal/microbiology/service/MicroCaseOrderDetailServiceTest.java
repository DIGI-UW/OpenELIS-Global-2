package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
        request.antibioticExposure = "Ceftriaxone started yesterday";
        request.criticalNotificationPreference = "Call attending immediately";

        MicroCaseOrderDetail saved = service.saveOrderDetail("case-1", request, "1");

        assertEquals("case-1", saved.getCaseId());
        assertEquals("Inpatient ward 3", saved.getPatientOrigin());
        assertEquals(Integer.valueOf(2), saved.getNumberOfSets());
        assertEquals("Fever, suspected sepsis", saved.getClinicalHistory());
        assertEquals("Ceftriaxone started yesterday", saved.getAntibioticExposure());
        assertEquals("Call attending immediately", saved.getCriticalNotificationPreference());
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
}
