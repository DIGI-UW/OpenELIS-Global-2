package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.*;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;

public class ReportImplementationFactoryIT extends BaseWebContextSensitiveTest {

    @Test
    public void testGetReportCreator() {
        assertNotNull("Creator for patientARV1 should not be null", ReportImplementationFactory.getReportCreator("patientARV1"));
        assertTrue("Creator for patientARV1 should be PatientARVVersion1Report", 
            ReportImplementationFactory.getReportCreator("patientARV1") instanceof PatientARVVersion1Report);
        
        assertNotNull("Creator for patientHaitiClinical should not be null", ReportImplementationFactory.getReportCreator("patientHaitiClinical"));
        assertTrue("Creator for patientHaitiClinical should be PatientClinicalReport", 
            ReportImplementationFactory.getReportCreator("patientHaitiClinical") instanceof PatientClinicalReport);
        
        assertNotNull("Creator for retroCINonConformityByDate should not be null", ReportImplementationFactory.getReportCreator("retroCINonConformityByDate"));
        assertTrue("Creator for retroCINonConformityByDate should be RetroCINonConformityByDate", 
            ReportImplementationFactory.getReportCreator("retroCINonConformityByDate") instanceof RetroCINonConformityByDate);
    }

    @Test
    public void testGetParameterSetter() {
        IReportParameterSetter setter = ReportImplementationFactory.getParameterSetter("patientARV1");
        assertNotNull("Setter for patientARV1 should not be null", setter);
        assertTrue("Setter for patientARV1 should be ReportSpecificationParameters", 
            setter instanceof ReportSpecificationParameters);
        
        IReportParameterSetter clinicalSetter = ReportImplementationFactory.getParameterSetter("patientHaitiClinical");
        assertNotNull("Setter for patientHaitiClinical should not be null", clinicalSetter);
        assertTrue("Setter for patientHaitiClinical should be PatientClinicalReport", 
            clinicalSetter instanceof PatientClinicalReport);
        
        IReportParameterSetter labnoSetter = ReportImplementationFactory.getParameterSetter("retroCINonConformityByLabno");
        assertNotNull("Setter for retroCINonConformityByLabno should not be null", labnoSetter);
        assertTrue("Setter for retroCINonConformityByLabno should be ReportSpecificationParameters", 
            labnoSetter instanceof ReportSpecificationParameters);
    }

    @Test
    public void testNonExistentReport() {
        assertNull("Non-existent report should return null creator", ReportImplementationFactory.getReportCreator("nonExistentReport"));
        assertNull("Non-existent report should return null setter", ReportImplementationFactory.getParameterSetter("nonExistentReport"));
    }
}
