/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.reports.action.implementation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralResult;
import org.openelisglobal.reports.action.implementation.reportBeans.ClinicalPatientData;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;

public class PatientCILNSPClinical extends PatientReport implements IReportCreator, IReportParameterSetter {

    private static Set<Integer> analysisStatusIds;
    protected List<ClinicalPatientData> clinicalReportItems;

    static {
        analysisStatusIds = new HashSet<>();
        analysisStatusIds.add(Integer
                .parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.BiologistRejected)));
        analysisStatusIds.add(
                Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized)));
        analysisStatusIds.add(Integer.parseInt(
                SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NonConforming_depricated)));
        analysisStatusIds.add(
                Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted)));
        analysisStatusIds.add(Integer
                .parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance)));
        analysisStatusIds.add(
                Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled)));
        analysisStatusIds.add(Integer
                .parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalRejected)));
    }

    static final String configName = ConfigurationProperties.getInstance().getPropertyValue(Property.configurationName);

    public PatientCILNSPClinical() {
        super();
    }

    @Override
    protected String reportFileName() {
        return "PatientReportCDI";
        // return "PatientClinicalReport";
    }

    @Override
    protected void createReportParameters() {
        super.createReportParameters();
        reportParameters.put("billingNumberLabel",
                SpringContext.getBean(LocalizationService.class).getLocalizedValueById(ConfigurationProperties
                        .getInstance().getPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL)));
        reportParameters.put("footerName", getFooterName());
    }

    private Object getFooterName() {
        if (configName.equals("CI IPCI") || configName.equals("CI LNSP")) {
            return "CILNSPFooter.jasper";
        } else {
            return "";
        }
    }

    @Override
    protected String getHeaderName() {
        return "CDIHeader.jasper";
    }

    @Override
    protected void createReportItems() {
        Set<SampleItem> sampleSet = new HashSet<>();

        boolean isConfirmationSample = sampleService.isConfirmationSample(currentSample);
        List<Analysis> analysisList = analysisService
                .getAnalysesBySampleIdAndStatusId(sampleService.getId(currentSample), analysisStatusIds);
        List<Analysis> filteredAnalysisList = userService.filterAnalysesByLabUnitRoles(systemUserId, analysisList,
                Constants.ROLE_REPORTS);
        List<ClinicalPatientData> currentSampleReportItems = new ArrayList<>(filteredAnalysisList.size());
        currentConclusion = null;
        for (Analysis analysis : filteredAnalysisList) {
            if (!analysis.getTest().isInLabOnly()) {
                boolean hasParentResult = analysis.getParentResult() != null;
                sampleSet.add(analysis.getSampleItem());
                if (analysis.getTest() != null) {
                    currentAnalysis = analysis;
                    ClinicalPatientData resultsData = buildClinicalPatientData(hasParentResult);
                    if (isConfirmationSample) {
                        String alerts = resultsData.getAlerts();
                        if (!GenericValidator.isBlankOrNull(alerts)) {
                            alerts += ", C";
                        } else {
                            alerts = "C";
                        }

                        resultsData.setAlerts(alerts);
                    }

                    if (currentAnalysis.isReferredOut()) {
                        Referral referral = referralService.getReferralByAnalysisId(currentAnalysis.getId());
                        if (referral != null) {
                            // addReferredTests method in both PatientClinical and PatientCILNSPClinical are
                            // nearly identical and
                            // should be refactored to use the same code.
                            List<ClinicalPatientData> referredData = addReferredTests(referral, resultsData);
                            currentSampleReportItems.addAll(referredData);
                        }
                    } else {
                        reportItems.add(resultsData);
                        currentSampleReportItems.add(resultsData);
                    }
                }
            }
        }
        setCollectionTime(sampleSet, currentSampleReportItems, true);
    }

    @Override
    protected void setEmptyResult(ClinicalPatientData data) {
        data.setAnalysisStatus(MessageUtil.getMessage("report.test.status.inProgress"));
    }

    @Override
    protected void setReferredOutResult(ClinicalPatientData data) {
        data.setAlerts("R");
        data.setAnalysisStatus(MessageUtil.getMessage("report.test.status.inProgress"));
    }

    // addReferredTests method in both PatientClinical and PatientCILNSPClinical are
    // nearly identical and
    // should be refactored to use the same code.
    private List<ClinicalPatientData> addReferredTests(Referral referral, ClinicalPatientData parentData) {
        List<ReferralResult> referralResults = referralResultService.getReferralResultsForReferral(referral.getId());
        NoteService noteService = SpringContext.getBean(NoteService.class);
        String note = noteService.getNotesAsString(currentAnalysis, false, true, "<br/>", FILTER, true);
        List<ClinicalPatientData> currentSampleReportItems = new ArrayList<>();

        if (!referralResults.isEmpty()) {
            boolean referralTestAssigned = false;
            for (ReferralResult referralResult : referralResults) {
                if (referralResult.getTestId() != null) {
                    referralTestAssigned = true;
                }
            }
            if (!referralTestAssigned) {
                reportItems.add(parentData);
                currentSampleReportItems.add(parentData);
            }
        } else {
            reportItems.add(parentData);
            currentSampleReportItems.add(parentData);
        }
        for (int i = 0; i < referralResults.size(); i++) {
            if (referralResults.get(i).getResult() == null) {
                sampleCompleteMap.put(convertToAlphaNumericDisplay(currentSample), Boolean.FALSE);
            } else {

                i = lastUsedReportReferralResultValue(referralResults, i);
                ReferralResult referralResult = referralResults.get(i);

                ClinicalPatientData data = new ClinicalPatientData();
                copyParentData(data, parentData);

                data.setResult(reportReferralResultValue);
                data.setNote(note);
                data.setSampleType(parentData.getSampleType());
                data.setSampleId(parentData.getSampleId());
                String testId = referralResult.getTestId();
                if (!GenericValidator.isBlankOrNull(testId)) {
                    Test test = new Test();
                    test.setId(testId);
                    testService.getData(test);
                    data.setTestName(TestServiceImpl.getUserLocalizedReportingTestName(test));

                    String uom = getUnitOfMeasure(test);
                    if (reportReferralResultValue != null) {
                        data.setReferralResult(addIfNotEmpty(reportReferralResultValue, uom));
                    }
                    data.setTestRefRange(addIfNotEmpty(getRange(referralResult.getResult()), uom));
                    data.setTestSortOrder(GenericValidator.isBlankOrNull(test.getSortOrder()) ? Integer.MAX_VALUE
                            : Integer.parseInt(test.getSortOrder()));
                    data.setSectionSortOrder(currentAnalysis.getTestSection().getSortOrderInt());
                    data.setTestSection(currentAnalysis.getTestSection().getLocalizedName());
                }

                if (GenericValidator.isBlankOrNull(reportReferralResultValue)) {
                    sampleCompleteMap.put(convertToAlphaNumericDisplay(currentSample), Boolean.FALSE);
                    data.setAnalysisStatus(MessageUtil.getMessage("report.test.status.inProgress"));
                } else {
                    data.setResult(reportReferralResultValue);
                }

                data.setAlerts(getResultFlag(referralResult.getResult(), null));
                data.setHasRangeAndUOM(
                        referralResult.getResult() != null && "N".equals(referralResult.getResult().getResultType()));

                reportItems.add(data);
                currentSampleReportItems.add(data);
            }
        }
        return currentSampleReportItems;
    }

    private void copyParentData(ClinicalPatientData data, ClinicalPatientData parentData) {
        data.setContactInfo(parentData.getContactInfo());
        data.setSiteInfo(parentData.getSiteInfo());
        data.setReceivedDate(parentData.getReceivedDate());
        data.setDob(parentData.getDob());
        data.setAge(parentData.getAge());
        data.setGender(parentData.getGender());
        data.setNationalId(parentData.getNationalId());
        data.setPatientName(parentData.getPatientName());
        data.setFirstName(parentData.getFirstName());
        data.setLastName(parentData.getLastName());
        data.setDept(parentData.getDept());
        data.setCommune(parentData.getCommune());
        data.setStNumber(parentData.getStNumber());
        data.setAccessionNumber(parentData.getAccessionNumber());
        data.setLabOrderType(parentData.getLabOrderType());
    }

    @Override
    protected void postSampleBuild() {
        if (reportItems.isEmpty()) {
            ClinicalPatientData reportItem = buildClinicalPatientData(false);
            reportItem.setTestSection(MessageUtil.getMessage("report.no.results"));
            clinicalReportItems.add(reportItem);
        } else {
            buildReport();
        }
    }

    private void buildReport() {
        Collections.sort(reportItems, new Comparator<ClinicalPatientData>() {
            @Override
            public int compare(ClinicalPatientData o1, ClinicalPatientData o2) {
                String o1AccessionNumber = AccessionNumberUtil
                        .getAccessionNumberFromSampleItemAccessionNumber(o1.getAccessionNumber());
                String o2AccessionNumber = AccessionNumberUtil
                        .getAccessionNumberFromSampleItemAccessionNumber(o2.getAccessionNumber());
                int accessionSort = o1AccessionNumber.compareTo(o2AccessionNumber);

                if (accessionSort != 0) {
                    return accessionSort;
                }

                int sectionSort = o1.getSectionSortOrder() - o2.getSectionSortOrder();

                if (sectionSort != 0) {
                    return sectionSort;
                }

                int sampleTypeSort = o1.getSampleType().compareTo(o2.getSampleType());

                if (sampleTypeSort != 0) {
                    return sampleTypeSort;
                }

                int sampleIdSort = o1.getSampleId().compareTo(o2.getSampleId());

                if (sampleIdSort != 0) {
                    return sampleIdSort;
                }

                if (o1.getParentResult() != null && o2.getParentResult() != null) {
                    int parentSort = Integer.parseInt(o1.getParentResult().getId())
                            - Integer.parseInt(o2.getParentResult().getId());
                    if (parentSort != 0) {
                        return parentSort;
                    }
                }
                return o1.getTestSortOrder() - o2.getTestSortOrder();
            }
        });

        ArrayList<ClinicalPatientData> augmentedList = new ArrayList<>(reportItems.size());
        HashSet<String> parentResults = new HashSet<>();
        for (ClinicalPatientData data : reportItems) {
            if (data.getParentResult() != null && !parentResults.contains(data.getParentResult().getId())) {
                parentResults.add(data.getParentResult().getId());
                ClinicalPatientData marker = new ClinicalPatientData(data);
                ResultService resultResultService = SpringContext.getBean(ResultService.class);
                Result result = data.getParentResult();
                marker.setTestName(resultResultService.getSimpleResultValue(result));
                marker.setResult(null);
                marker.setTestRefRange(null);
                marker.setParentMarker(true);
                augmentedList.add(marker);
            }

            augmentedList.add(data);
        }

        reportItems = augmentedList;

        String currentPanelId = null;
        for (ClinicalPatientData reportItem : reportItems) {
            if (reportItem.getPanel() != null && !reportItem.getPanel().getId().equals(currentPanelId)) {
                currentPanelId = reportItem.getPanel().getId();
                reportItem.setSeparator(true);
            } else if (reportItem.getPanel() == null && currentPanelId != null) {
                currentPanelId = null;
                reportItem.setSeparator(true);
            }

            int dividerIndex = reportItem.getAccessionNumber().lastIndexOf("-");
            reportItem.setAccessionNumber(reportItem.getAccessionNumber().substring(0, dividerIndex));
            reportItem.setCompleteFlag(MessageUtil
                    .getMessage(sampleCompleteMap.get(reportItem.getAccessionNumber()) ? "report.status.complete"
                            : "report.status.partial"));
            if (reportItem.isCorrectedResult()) {
                if (reportItem.getNote() != null && reportItem.getNote().length() > 0) {
                    reportItem.setNote(MessageUtil.getMessage("result.corrected") + "<br/>" + reportItem.getNote());
                } else {
                    reportItem.setNote(MessageUtil.getMessage("result.corrected"));
                }
            }

            reportItem
                    .setCorrectedResult(sampleCorrectedMap.get(reportItem.getAccessionNumber().split("_")[0]) != null);
        }
    }

    @Override
    protected String getReportNameForParameterPage() {
        return MessageUtil.getMessage("openreports.patientTestStatus");
    }

    @Override
    public JRDataSource getReportDataSource() throws IllegalStateException {
        if (!initialized) {
            throw new IllegalStateException("initializeReport not called first");
        }

        return errorFound ? new JRBeanCollectionDataSource(errorMsgs) : new JRBeanCollectionDataSource(reportItems);
    }

    @Override
    protected void initializeReportItems() {
        super.initializeReportItems();
        clinicalReportItems = new ArrayList<>();
    }

    @Override
    protected void setReferredResult(ClinicalPatientData data, Result result) {
        data.setResult(data.getResult());
        data.setAlerts(getResultFlag(result, null));
    }

    @Override
    protected boolean appendUOMToRange() {
        return false;
    }

    @Override
    protected boolean augmentResultWithFlag() {
        return false;
    }

    @Override
    protected boolean useReportingDescription() {
        return true;
    }
}
