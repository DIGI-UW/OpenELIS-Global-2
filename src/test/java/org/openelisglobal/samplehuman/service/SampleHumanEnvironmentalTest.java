package org.openelisglobal.samplehuman.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.dao.SampleHumanDAO;

public class SampleHumanEnvironmentalTest {
    @Test
    public void environmentalOrdersNeverSupplyDemographicsToClinicalResultRules() {
        var dao = mock(SampleHumanDAO.class);
        var service = new SampleHumanServiceImpl();
        service.baseObjectDAO = dao;
        var sample = new Sample();
        for (String domain : new String[] { "E", "ENVIRONMENTAL" }) {
            sample.setDomain(domain);
            assertNull(service.getPatientForSample(sample));
        }
        verifyZeroInteractions(dao);
        sample.setDomain("H");
        var patient = new org.openelisglobal.patient.valueholder.Patient();
        when(dao.getPatientForSample(sample)).thenReturn(patient);
        assertSame(patient, service.getPatientForSample(sample));
    }
}
