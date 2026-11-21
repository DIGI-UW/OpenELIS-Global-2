package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;

public class ReportImplementationFactoryTest extends BaseWebContextSensitiveTest {

    @Test
    public void testAllMenuReportsHaveCreators() {
        String[] menuReportNames = {
                // Routine reports
                "patientCILNSP_vreduit", "statisticsReport", "indicatorHaitiLNSPAllTests", "indicatorCDILNSPHIV",
                "sampleRejectionReport", "activityReportByTest", "activityReportByPanel", "activityReportByTestSection",
                "referredOut", "haitiNonConformityByDate", "haitiNonConformityBySectionReason", "validationBacklog",
                "CISampleRoutineExport",

                // Study reports
                "patientARVInitial1", "patientARVInitial2", "patientARVFollowup1", "patientARVFollowup2", "patientARV1",
                "patientEID1", "patientEID2", "patientVL1", "patientIndeterminate1", "patientIndeterminate2",
                "patientIndeterminateByLocation", "patientSpecialReport", "patientCollection", "patientAssociated",
                "indicatorSectionPerformance", "retroCINonConformityByDate", "retroCInonConformityBySectionReason",
                "retroCINonConformityByLabno", "retroCInonConformityNotification", "retroCIFollowupRequiredByLocation",
                "CIStudyExport", "Trends" };

        for (String reportName : menuReportNames) {
            assertNotNull("No report creator registered for menu report: " + reportName,
                    ReportImplementationFactory.getReportCreator(reportName));
        }
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

        IReportParameterSetter labnoSetter = ReportImplementationFactory
                .getParameterSetter("retroCINonConformityByLabno");
        assertNotNull("Setter for retroCINonConformityByLabno should not be null", labnoSetter);
        assertTrue("Setter for retroCINonConformityByLabno should be ReportSpecificationParameters",
                labnoSetter instanceof ReportSpecificationParameters);
    }

    @Test
    public void testNonExistentReport() {
        assertNull("Non-existent report should return null creator",
                ReportImplementationFactory.getReportCreator("nonExistentReport"));
        assertNull("Non-existent report should return null setter",
                ReportImplementationFactory.getParameterSetter("nonExistentReport"));
    }
}
