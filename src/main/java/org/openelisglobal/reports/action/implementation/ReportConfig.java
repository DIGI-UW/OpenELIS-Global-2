package org.openelisglobal.reports.action.implementation;

import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.action.implementation.ReportSpecificationParameters.Parameter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class ReportConfig {

    private static final boolean isLNSP = true;

    // --- Parameter Setters ---

    @Bean("patientARV1")
    @Scope("prototype")
    public IReportParameterSetter patientARV1ParameterSetter() {
        return new ReportSpecificationParameters(Parameter.ACCESSION_RANGE,
                MessageUtil.getMessage("reports.label.patient.ARV.all"), null);
    }

    @Bean("retroCINonConformityByLabno")
    @Scope("prototype")
    public IReportParameterSetter retroCINonConformityByLabnoParameterSetter() {
        return new ReportSpecificationParameters(Parameter.ACCESSION_RANGE,
                MessageUtil.getMessage("reports.label.patient.ARV.all"), null);
    }

    @Bean("patientARVInitial1")
    @Scope("prototype")
    public IReportParameterSetter patientARVInitial1ParameterSetter() {
        return new ReportSpecificationParameters(Parameter.ACCESSION_RANGE,
                MessageUtil.getMessage("reports.label.patient.ARV.initial"), null);
    }

    @Bean("patientARVInitial2")
    @Scope("prototype")
    public IReportParameterSetter patientARVInitial2ParameterSetter() {
        return new ReportSpecificationParameters(Parameter.ACCESSION_RANGE,
                MessageUtil.getMessage("reports.label.patient.ARV.initial"), null);
    }

    @Bean("patientARVFollowup1")
    @Scope("prototype")
    public IReportParameterSetter patientARVFollowup1ParameterSetter() {
        return new ReportSpecificationParameters(Parameter.ACCESSION_RANGE,
                MessageUtil.getMessage("reports.label.patient.ARV.followup"), null);
    }

    @Bean("patientARVFollowup2")
    @Scope("prototype")
    public IReportParameterSetter patientARVFollowup2ParameterSetter() {
        return new ReportSpecificationParameters(Parameter.ACCESSION_RANGE,
                MessageUtil.getMessage("reports.label.patient.ARV.followup"), null);
    }

    @Bean("patientEID1")
    @Scope("prototype")
    public IReportParameterSetter patientEID1ParameterSetter() {
        return new ReportSpecificationParameters(new Parameter[] { Parameter.ACCESSION_RANGE,
                Parameter.USE_PATIENT_SEARCH, Parameter.USE_SITE_SEARCH },
                MessageUtil.getMessage("reports.label.patient.EID"), null);
    }

    @Bean("patientEID2")
    @Scope("prototype")
    public IReportParameterSetter patientEID2ParameterSetter() {
        return new ReportSpecificationParameters(Parameter.ACCESSION_RANGE,
                MessageUtil.getMessage("reports.label.patient.EID"), null);
    }

    @Bean("patientVL1")
    @Scope("prototype")
    public IReportParameterSetter patientVL1ParameterSetter() {
        return new ReportSpecificationParameters(new Parameter[] { Parameter.ACCESSION_RANGE,
                Parameter.USE_PATIENT_SEARCH, Parameter.USE_SITE_SEARCH },
                MessageUtil.getMessage("reports.label.patient.VL"), null);
    }

    @Bean("patientIndeterminate1")
    @Scope("prototype")
    public IReportParameterSetter patientIndeterminate1ParameterSetter() {
        return new ReportSpecificationParameters(Parameter.ACCESSION_RANGE,
                MessageUtil.getMessage("reports.label.patient.indeterminate"), null);
    }

    @Bean("patientIndeterminate2")
    @Scope("prototype")
    public IReportParameterSetter patientIndeterminate2ParameterSetter() {
        return new ReportSpecificationParameters(Parameter.ACCESSION_RANGE,
                MessageUtil.getMessage("reports.label.patient.indeterminate"), null);
    }

    @Bean("indicatorSectionPerformance")
    @Scope("prototype")
    public IReportParameterSetter indicatorSectionPerformanceParameterSetter() {
        return new ReportSpecificationParameters(Parameter.NO_SPECIFICATION,
                MessageUtil.getMessage("reports.label.indicator.performance"), null);
    }

    @Bean("HaitiExportReport")
    @Scope("prototype")
    public IReportParameterSetter haitiExportReportParameterSetter() {
        return new ReportSpecificationParameters(Parameter.DATE_RANGE,
                MessageUtil.getMessage("reports.label.project.export") + " "
                        + MessageUtil.getContextualMessage("sample.collectionDate"),
                null);
    }

    @Bean("HaitiLNSPExportReport")
    @Scope("prototype")
    public IReportParameterSetter haitiLNSPExportReportParameterSetter() {
        return new ReportSpecificationParameters(Parameter.DATE_RANGE,
                MessageUtil.getMessage("reports.label.project.export") + " "
                        + MessageUtil.getContextualMessage("sample.collectionDate"),
                null);
    }

    @Bean("retroCINonConformityByDate")
    @Scope("prototype")
    public IReportParameterSetter retroCINonConformityByDateParameterSetter() {
        return new ReportSpecificationParameters(Parameter.DATE_RANGE,
                MessageUtil.getMessage("openreports.nonConformityReport"), null);
    }

    @Bean("haitiNonConformityByDate")
    @Scope("prototype")
    public IReportParameterSetter haitiNonConformityByDateParameterSetter() {
        return new ReportSpecificationParameters(Parameter.DATE_RANGE,
                MessageUtil.getMessage("openreports.nonConformityReport"), null);
    }

    @Bean("haitiClinicalNonConformityByDate")
    @Scope("prototype")
    public IReportParameterSetter haitiClinicalNonConformityByDateParameterSetter() {
        return new ReportSpecificationParameters(Parameter.DATE_RANGE,
                MessageUtil.getMessage("openreports.nonConformityReport"), null);
    }

    @Bean("retroCInonConformityBySectionReason")
    @Scope("prototype")
    public IReportParameterSetter retroCInonConformityBySectionReasonParameterSetter() {
        return new ReportSpecificationParameters(Parameter.DATE_RANGE,
                MessageUtil.getMessage("reports.nonConformity.bySectionReason.title"), null);
    }

    @Bean("haitiNonConformityBySectionReason")
    @Scope("prototype")
    public IReportParameterSetter haitiNonConformityBySectionReasonParameterSetter() {
        return new ReportSpecificationParameters(Parameter.DATE_RANGE,
                MessageUtil.getMessage("reports.nonConformity.bySectionReason.title"), null);
    }

    @Bean("haitiClinicalNonConformityBySectionReason")
    @Scope("prototype")
    public IReportParameterSetter haitiClinicalNonConformityBySectionReasonParameterSetter() {
        return new ReportSpecificationParameters(Parameter.DATE_RANGE,
                MessageUtil.getMessage("reports.nonConformity.bySectionReason.title"), null);
    }

    @Bean("patientSpecialReport")
    @Scope("prototype")
    public IReportParameterSetter patientSpecialReportParameterSetter() {
        return new ReportSpecificationParameters(Parameter.ACCESSION_RANGE,
                MessageUtil.getMessage("reports.specialRequest.title"), null);
    }

    @Bean("indicatorRealisation")
    @Scope("prototype")
    public IReportParameterSetter indicatorRealisationParameterSetter() {
        return new ReportSpecificationParameters(Parameter.DATE_RANGE,
                MessageUtil.getMessage("report.realisation"), null);
    }

    @Bean("epiSurveillanceExport")
    @Scope("prototype")
    public IReportParameterSetter epiSurveillanceExportParameterSetter() {
        return new ReportSpecificationParameters(Parameter.DATE_RANGE,
                MessageUtil.getMessage("banner.menu.report.epi.surveillance.export"), null);
    }

    // --- Report Creators ---

    @Bean("patientHaitiClinicalCreator")
    @Scope("prototype")
    public IReportCreator patientHaitiClinicalCreator() {
        return new PatientClinicalReport(!isLNSP);
    }

    @Bean("patientHaitiLNSPCreator")
    @Scope("prototype")
    public IReportCreator patientHaitiLNSPCreator() {
        return new PatientClinicalReport(isLNSP);
    }

    @Bean("patientCILNSP_vreduitCreator")
    @Scope("prototype")
    public IReportCreator patientCILNSP_vreduitCreator() {
        return new PatientCILNSPClinical_vreduit();
    }

    // --- Simple Bean Definitions (Mapping names to classes) ---

    @Bean("patientIndeterminateByLocation")
    @Scope("prototype")
    public IReportParameterSetter patientIndeterminateByLocation() {
        return new PatientIndeterminateByLocationReport();
    }

    @Bean("patientHaitiClinical")
    @Scope("prototype")
    public IReportParameterSetter patientHaitiClinical() {
        return new PatientClinicalReport();
    }

    @Bean("patientHaitiLNSP")
    @Scope("prototype")
    public IReportParameterSetter patientHaitiLNSP() {
        return new PatientClinicalReport();
    }

    @Bean("patientCILNSP")
    @Scope("prototype")
    public IReportParameterSetter patientCILNSP() {
        return new PatientClinicalReport();
    }

    @Bean("patientCILNSP_vreduit")
    @Scope("prototype")
    public IReportParameterSetter patientCILNSP_vreduit() {
        return new PatientClinicalReport();
    }

    @Bean("TBPatientReport")
    @Scope("prototype")
    public IReportParameterSetter tbPatientReport() {
        return new PatientClinicalReport();
    }

    @Bean("indicatorHaitiClinicalHIV")
    @Scope("prototype")
    public IReportParameterSetter indicatorHaitiClinicalHIV() {
        return new IndicatorHIV();
    }

    @Bean("indicatorHaitiLNSPHIV")
    @Scope("prototype")
    public IReportParameterSetter indicatorHaitiLNSPHIV() {
        return new IndicatorHIVLNSP();
    }

    @Bean("indicatorCDIHIVLNSP")
    @Scope("prototype")
    public IReportParameterSetter indicatorCDIHIVLNSP() {
        return new IndicatorCDIHIVLNSP();
    }

    @Bean("indicatorHaitiClinicalAllTests")
    @Scope("prototype")
    public IReportParameterSetter indicatorHaitiClinicalAllTests() {
        return new IndicatorAllTestClinical();
    }

    @Bean("indicatorHaitiLNSPAllTests")
    @Scope("prototype")
    public IReportParameterSetter indicatorHaitiLNSPAllTests() {
        return new IndicatorAllTestLNSP();
    }

    @Bean("CISampleExport")
    @Scope("prototype")
    public IReportParameterSetter ciSampleExport() {
        return new ExportProjectByDate();
    }

    @Bean("ForCIDashboard")
    @Scope("prototype")
    public IReportParameterSetter forCIDashboard() {
        return new ForCIDashboard();
    }

    @Bean("CISampleRoutineExport")
    @Scope("prototype")
    public IReportParameterSetter ciSampleRoutineExport() {
        return new ExportRoutineByDate();
    }

    @Bean("referredOut")
    @Scope("prototype")
    public IReportParameterSetter referredOut() {
        return new ReferredOutReport();
    }

    @Bean("indicatorConfirmation")
    @Scope("prototype")
    public IReportParameterSetter indicatorConfirmation() {
        return new ConfirmationReport();
    }

    @Bean("indicatorHaitiLNSPSiteTestCount")
    @Scope("prototype")
    public IReportParameterSetter indicatorHaitiLNSPSiteTestCount() {
        return new IndicatorHaitiSiteTestCountReport();
    }

    @Bean("retroCIFollowupRequiredByLocation")
    @Scope("prototype")
    public IReportParameterSetter retroCIFollowupRequiredByLocation() {
        return new RetroCIFollowupRequiredByLocation();
    }

    @Bean("retroCInonConformityNotification")
    @Scope("prototype")
    public IReportParameterSetter retroCInonConformityNotification() {
        return new RetroCINonConformityNotification();
    }

    @Bean("patientCollection")
    @Scope("prototype")
    public IReportParameterSetter patientCollection() {
        return new RetroCIPatientCollectionReport();
    }

    @Bean("patientAssociated")
    @Scope("prototype")
    public IReportParameterSetter patientAssociated() {
        return new RetroCIPatientAssociatedReport();
    }

    @Bean("activityReportByPanel")
    @Scope("prototype")
    public IReportParameterSetter activityReportByPanel() {
        return new ActivityReportByPanel();
    }

    @Bean("activityReportByTest")
    @Scope("prototype")
    public IReportParameterSetter activityReportByTest() {
        return new ActivityReportByTest();
    }

    @Bean("activityReportByTestSection")
    @Scope("prototype")
    public IReportParameterSetter activityReportByTestSection() {
        return new ActivityReportByTestSection();
    }

    @Bean("rejectionReportByPanel")
    @Scope("prototype")
    public IReportParameterSetter rejectionReportByPanel() {
        return new RejectionReportByPanel();
    }

    @Bean("rejectionReportByTest")
    @Scope("prototype")
    public IReportParameterSetter rejectionReportByTest() {
        return new RejectionReportByTest();
    }

    @Bean("rejectionReportByTestSection")
    @Scope("prototype")
    public IReportParameterSetter rejectionReportByTestSection() {
        return new RejectionReportByTestSection();
    }

    @Bean("CIStudyExport")
    @Scope("prototype")
    public IReportParameterSetter ciStudyExport() {
        return new ExportStudyProjectByDate();
    }

    @Bean("TBOrderExport")
    @Scope("prototype")
    public IReportParameterSetter tbOrderExport() {
        return new ExportTBOrdersByDate();
    }

    @Bean("TBOrderReport")
    @Scope("prototype")
    public IReportParameterSetter tbOrderReportParameter() {
        return new TBOrderReport();
    }

    @Bean("Trends")
    @Scope("prototype")
    public IReportParameterSetter trends() {
        return new ExportTrendsByDate();
    }

    @Bean("ExportWHONETReportByDate")
    @Scope("prototype")
    public IReportParameterSetter exportWHONETReportByDate() {
        return new WHONETExportRoutineByDate();
    }

    @Bean("covidResultsReport")
    @Scope("prototype")
    public IReportParameterSetter covidResultsReport() {
        return new CovidResultsReport();
    }

    @Bean("statisticsReport")
    @Scope("prototype")
    public IReportParameterSetter statisticsReport() {
        return new StatisticsReport();
    }

    @Bean("sampleRejectionReport")
    @Scope("prototype")
    public IReportParameterSetter sampleRejectionReport() {
        return new CSVSampleRejectionReport();
    }

    // --- Creator Beans ---

    @Bean("patientARV1Creator")
    @Scope("prototype")
    public IReportCreator patientARV1Creator() {
        return new PatientARVVersion1Report();
    }

    @Bean("retroCINonConformityByLabnoCreator")
    @Scope("prototype")
    public IReportCreator retroCINonConformityByLabnoCreator() {
        return new RetroCINonConformityByLabno();
    }

    @Bean("patientARVInitial1Creator")
    @Scope("prototype")
    public IReportCreator patientARVInitial1Creator() {
        return new PatientARVInitialVersion1Report();
    }

    @Bean("patientARVInitial2Creator")
    @Scope("prototype")
    public IReportCreator patientARVInitial2Creator() {
        return new PatientARVInitialVersion2Report();
    }

    @Bean("patientARVFollowup1Creator")
    @Scope("prototype")
    public IReportCreator patientARVFollowup1Creator() {
        return new PatientARVFollowupVersion1Report();
    }

    @Bean("patientARVFollowup2Creator")
    @Scope("prototype")
    public IReportCreator patientARVFollowup2Creator() {
        return new PatientARVFollowupVersion2Report();
    }

    @Bean("patientEID1Creator")
    @Scope("prototype")
    public IReportCreator patientEID1Creator() {
        return new PatientEIDVersion1Report();
    }

    @Bean("patientEID2Creator")
    @Scope("prototype")
    public IReportCreator patientEID2Creator() {
        return new PatientEIDVersion2Report();
    }

    @Bean("patientVL1Creator")
    @Scope("prototype")
    public IReportCreator patientVL1Creator() {
        return new PatientVLVersion1Report();
    }

    @Bean("patientIndeterminate1Creator")
    @Scope("prototype")
    public IReportCreator patientIndeterminate1Creator() {
        return new PatientIndeterminateVersion1Report();
    }

    @Bean("patientIndeterminate2Creator")
    @Scope("prototype")
    public IReportCreator patientIndeterminate2Creator() {
        return new PatientIndeterminateVersion2Report();
    }

    @Bean("patientIndeterminateByLocationCreator")
    @Scope("prototype")
    public IReportCreator patientIndeterminateByLocationCreator() {
        return new PatientIndeterminateByLocationReport();
    }

    @Bean("indicatorSectionPerformanceCreator")
    @Scope("prototype")
    public IReportCreator indicatorSectionPerformanceCreator() {
        return new IndicatorSectionPerformanceReport();
    }

    @Bean("patientCILNSPCreator")
    @Scope("prototype")
    public IReportCreator patientCILNSPCreator() {
        return new PatientCILNSPClinical();
    }

    @Bean("TBPatientReportCreator")
    @Scope("prototype")
    public IReportCreator tbPatientReportCreator() {
        return new TBPatientReport();
    }

    @Bean("indicatorHaitiClinicalHIVCreator")
    @Scope("prototype")
    public IReportCreator indicatorHaitiClinicalHIVCreator() {
        return new IndicatorHIV();
    }

    @Bean("indicatorHaitiLNSPHIVCreator")
    @Scope("prototype")
    public IReportCreator indicatorHaitiLNSPHIVCreator() {
        return new IndicatorHIVLNSP();
    }

    @Bean("indicatorHaitiClinicalAllTestsCreator")
    @Scope("prototype")
    public IReportCreator indicatorHaitiClinicalAllTestsCreator() {
        return new IndicatorAllTestClinical();
    }

    @Bean("indicatorHaitiLNSPAllTestsCreator")
    @Scope("prototype")
    public IReportCreator indicatorHaitiLNSPAllTestsCreator() {
        return new IndicatorAllTestLNSP();
    }

    @Bean("CISampleExportCreator")
    @Scope("prototype")
    public IReportCreator ciSampleExportCreator() {
        return new ExportProjectByDate();
    }

    @Bean("ForCIDashboardCreator")
    @Scope("prototype")
    public IReportCreator forCIDashboardCreator() {
        return new ForCIDashboard();
    }

    @Bean("CISampleRoutineExportCreator")
    @Scope("prototype")
    public IReportCreator ciSampleRoutineExportCreator() {
        return new ExportRoutineByDate();
    }

    @Bean("referredOutCreator")
    @Scope("prototype")
    public IReportCreator referredOutCreator() {
        return new ReferredOutReport();
    }

    @Bean("HaitiExportReportCreator")
    @Scope("prototype")
    public IReportCreator haitiExportReportCreator() {
        return new HaitiExportReport();
    }

    @Bean("HaitiLNSPExportReportCreator")
    @Scope("prototype")
    public IReportCreator haitiLNSPExportReportCreator() {
        return new HaitiLNSPExportReport();
    }

    @Bean("indicatorConfirmationCreator")
    @Scope("prototype")
    public IReportCreator indicatorConfirmationCreator() {
        return new ConfirmationReport();
    }

    @Bean("retroCINonConformityByDateCreator")
    @Scope("prototype")
    public IReportCreator retroCINonConformityByDateCreator() {
        return new RetroCINonConformityByDate();
    }

    @Bean("haitiNonConformityByDateCreator")
    @Scope("prototype")
    public IReportCreator haitiNonConformityByDateCreator() {
        return new HaitiNonConformityByDate();
    }

    @Bean("haitiClinicalNonConformityByDateCreator")
    @Scope("prototype")
    public IReportCreator haitiClinicalNonConformityByDateCreator() {
        return new HaitiNonConformityByDate();
    }

    @Bean("retroCInonConformityBySectionReasonCreator")
    @Scope("prototype")
    public IReportCreator retroCInonConformityBySectionReasonCreator() {
        return new RetroCINonConformityBySectionReason();
    }

    @Bean("haitiNonConformityBySectionReasonCreator")
    @Scope("prototype")
    public IReportCreator haitiNonConformityBySectionReasonCreator() {
        return new HaitiNonConformityBySectionReason();
    }

    @Bean("haitiClinicalNonConformityBySectionReasonCreator")
    @Scope("prototype")
    public IReportCreator haitiClinicalNonConformityBySectionReasonCreator() {
        return new HaitiNonConformityBySectionReason();
    }

    @Bean("indicatorHaitiLNSPSiteTestCountCreator")
    @Scope("prototype")
    public IReportCreator indicatorHaitiLNSPSiteTestCountCreator() {
        return new IndicatorHaitiSiteTestCountReport();
    }

    @Bean("retroCIFollowupRequiredByLocationCreator")
    @Scope("prototype")
    public IReportCreator retroCIFollowupRequiredByLocationCreator() {
        return new RetroCIFollowupRequiredByLocation();
    }

    @Bean("patientSpecialReportCreator")
    @Scope("prototype")
    public IReportCreator patientSpecialReportCreator() {
        return new PatientSpecialRequestReport();
    }

    @Bean("retroCInonConformityNotificationCreator")
    @Scope("prototype")
    public IReportCreator retroCInonConformityNotificationCreator() {
        return new RetroCINonConformityNotification();
    }

    @Bean("patientCollectionCreator")
    @Scope("prototype")
    public IReportCreator patientCollectionCreator() {
        return new RetroCIPatientCollectionReport();
    }

    @Bean("patientAssociatedCreator")
    @Scope("prototype")
    public IReportCreator patientAssociatedCreator() {
        return new RetroCIPatientAssociatedReport();
    }

    @Bean("indicatorCDIHIVLNSPCreator")
    @Scope("prototype")
    public IReportCreator indicatorCDIHIVLNSPCreator() {
        return new IndicatorCDIHIVLNSP();
    }

    @Bean("validationBacklogCreator")
    @Scope("prototype")
    public IReportCreator validationBacklogCreator() {
        return new ValidationBacklogReport();
    }

    @Bean("indicatorRealisationCreator")
    @Scope("prototype")
    public IReportCreator indicatorRealisationCreator() {
        return new IPCIRealisationReport();
    }

    @Bean("epiSurveillanceExportCreator")
    @Scope("prototype")
    public IReportCreator epiSurveillanceExportCreator() {
        return new HaitiLnspEpiExportReport();
    }

    @Bean("activityReportByPanelCreator")
    @Scope("prototype")
    public IReportCreator activityReportByPanelCreator() {
        return new ActivityReportByPanel();
    }

    @Bean("activityReportByTestCreator")
    @Scope("prototype")
    public IReportCreator activityReportByTestCreator() {
        return new ActivityReportByTest();
    }

    @Bean("activityReportByTestSectionCreator")
    @Scope("prototype")
    public IReportCreator activityReportByTestSectionCreator() {
        return new ActivityReportByTestSection();
    }

    @Bean("rejectionReportByPanelCreator")
    @Scope("prototype")
    public IReportCreator rejectionReportByPanelCreator() {
        return new RejectionReportByPanel();
    }

    @Bean("rejectionReportByTestCreator")
    @Scope("prototype")
    public IReportCreator rejectionReportByTestCreator() {
        return new RejectionReportByTest();
    }

    @Bean("rejectionReportByTestSectionCreator")
    @Scope("prototype")
    public IReportCreator rejectionReportByTestSectionCreator() {
        return new RejectionReportByTestSection();
    }

    @Bean("CIStudyExportCreator")
    @Scope("prototype")
    public IReportCreator ciStudyExportCreator() {
        return new ExportStudyProjectByDate();
    }

    @Bean("TrendsCreator")
    @Scope("prototype")
    public IReportCreator trendsCreator() {
        return new ExportTrendsByDate();
    }

    @Bean("TBOrderExportCreator")
    @Scope("prototype")
    public IReportCreator tbOrderExportCreator() {
        return new ExportTBOrdersByDate();
    }

    @Bean("MauritiusProtocolSheetCreator")
    @Scope("prototype")
    public IReportCreator mauritiusProtocolSheetCreator() {
        return new MauritiusProtocolSheet();
    }

    @Bean("ExportWHONETReportByDateCreator")
    @Scope("prototype")
    public IReportCreator exportWHONETReportByDateCreator() {
        return new WHONETExportRoutineByDate();
    }

    @Bean("covidResultsReportCreator")
    @Scope("prototype")
    public IReportCreator covidResultsReportCreator() {
        return new CovidResultsReport();
    }

    @Bean("statisticsReportCreator")
    @Scope("prototype")
    public IReportCreator statisticsReportCreator() {
        return new StatisticsReport();
    }

    @Bean("sampleRejectionReportCreator")
    @Scope("prototype")
    public IReportCreator sampleRejectionReportCreator() {
        return new CSVSampleRejectionReport();
    }

    @Bean("PatientPathologyReportCreator")
    @Scope("prototype")
    public IReportCreator patientPathologyReportCreator() {
        return new PatientPathologyReport();
    }

    @Bean("PatientCytologyReportCreator")
    @Scope("prototype")
    public IReportCreator patientCytologyReportCreator() {
        return new PatientCytologyReport();
    }

    @Bean("PatientImmunoChemistryReportCreator")
    @Scope("prototype")
    public IReportCreator patientImmunoChemistryReportCreator() {
        return new PatientImmunoChemistryReport();
    }

    @Bean("DualInSituHybridizationReportCreator")
    @Scope("prototype")
    public IReportCreator dualInSituHybridizationReportCreator() {
        return new DualInSituHybridizationReport();
    }

    @Bean("BreastCancerHormoneReceptorReportCreator")
    @Scope("prototype")
    public IReportCreator breastCancerHormoneReceptorReportCreator() {
        return new BreastCancerHormoneReceptorReport();
    }
}
