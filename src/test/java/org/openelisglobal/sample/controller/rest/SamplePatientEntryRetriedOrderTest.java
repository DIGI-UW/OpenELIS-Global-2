package org.openelisglobal.sample.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.patient.action.IPatientUpdate.PatientUpdateStatus;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;

/**
 * FR-K3: a new-order save retried after its reply was lost carries the same
 * order key, and is applied to the order the first attempt created instead of
 * failing with "accession number already in use" or adding the patient again.
 */
@RunWith(MockitoJUnitRunner.class)
public class SamplePatientEntryRetriedOrderTest {

    private static final String LAB_NUMBER = "DEV01260000000000001";

    @Mock
    private SampleService sampleService;
    @Mock
    private SampleHumanService sampleHumanService;

    @InjectMocks
    private SamplePatientEntryRestController controller;

    @Test
    public void aRetryWithTheSameKeyUpdatesTheOrderAndReusesItsPatient() {
        UUID key = UUID.randomUUID();
        Sample existing = existingSample(key);
        Patient patient = new Patient();
        patient.setId("88");
        when(sampleService.getSampleByAccessionNumber(LAB_NUMBER)).thenReturn(existing);
        when(sampleHumanService.getPatientForSample(existing)).thenReturn(patient);
        SampleOrderItem order = newOrder(key.toString());
        PatientManagementInfo patientInfo = addedPatient();

        controller.resolveRetriedOrder(order, patientInfo);

        assertEquals("41", order.getSampleId());
        assertEquals("88", patientInfo.getPatientPK());
        assertEquals(PatientUpdateStatus.NO_ACTION, patientInfo.getPatientUpdateStatus());
    }

    @Test
    public void aRetryForAnUpdatedPatientDoesNotUpdateItAgain() {
        UUID key = UUID.randomUUID();
        Sample existing = existingSample(key);
        Patient patient = new Patient();
        patient.setId("88");
        when(sampleService.getSampleByAccessionNumber(LAB_NUMBER)).thenReturn(existing);
        when(sampleHumanService.getPatientForSample(existing)).thenReturn(patient);
        SampleOrderItem order = newOrder(key.toString());
        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientPK("88");
        patientInfo.setPatientUpdateStatus(PatientUpdateStatus.UPDATE);

        controller.resolveRetriedOrder(order, patientInfo);

        assertEquals("41", order.getSampleId());
        assertEquals(PatientUpdateStatus.NO_ACTION, patientInfo.getPatientUpdateStatus());
    }

    @Test
    public void aDifferentKeyLeavesTheSaveToBeRejectedAsADuplicateLabNumber() {
        when(sampleService.getSampleByAccessionNumber(LAB_NUMBER)).thenReturn(existingSample(UUID.randomUUID()));
        SampleOrderItem order = newOrder(UUID.randomUUID().toString());
        PatientManagementInfo patientInfo = addedPatient();

        controller.resolveRetriedOrder(order, patientInfo);

        assertNull(order.getSampleId());
        assertEquals(PatientUpdateStatus.ADD, patientInfo.getPatientUpdateStatus());
        verify(sampleHumanService, never()).getPatientForSample(any());
    }

    @Test
    public void aSaveWithoutAKeyIsUntouched() {
        SampleOrderItem order = newOrder(null);

        controller.resolveRetriedOrder(order, addedPatient());

        assertNull(order.getSampleId());
        verify(sampleService, never()).getSampleByAccessionNumber(anyString());
    }

    private Sample existingSample(UUID key) {
        Sample sample = new Sample();
        sample.setId("41");
        sample.setAccessionNumber(LAB_NUMBER);
        sample.setFhirUuid(key);
        return sample;
    }

    private SampleOrderItem newOrder(String orderKey) {
        SampleOrderItem order = new SampleOrderItem();
        order.setLabNo(LAB_NUMBER);
        order.setOrderKey(orderKey);
        return order;
    }

    private PatientManagementInfo addedPatient() {
        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientUpdateStatus(PatientUpdateStatus.ADD);
        return patientInfo;
    }
}
