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
            assertTrue("No report creator registered for menu report: " + reportName,
                    isCreatorBean(reportName + "Creator"));
        }
    }

    @Test
    public void creatorStaysTheCreatorBeanAfterTheParameterPageWasBuilt() {
        assertTrue(ReportImplementationFactory.getParameterSetter("patientCILNSP") instanceof PatientClinicalReport);

        IReportCreator creator = ReportImplementationFactory.getReportCreator("patientCILNSP");

        assertEquals(PatientCILNSPClinical.class, creator.getClass());
    }

    private boolean isCreatorBean(String beanName) {
        return webApplicationContext.containsBean(beanName)
                && webApplicationContext.isTypeMatch(beanName, IReportCreator.class);
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

    @Test
    public void testLegacyWhonetExportIsUnavailable() {
        // OGC-782 M4: Prove the legacy WHONET reports path cannot be resolved
        assertNull("The legacy WHONET export route should not return a creator",
                ReportImplementationFactory.getReportCreator("ExportWHONETReportByDate"));

        assertNull("The legacy WHONET export route should not return a parameter setter",
                ReportImplementationFactory.getParameterSetter("ExportWHONETReportByDate"));
    }
}
