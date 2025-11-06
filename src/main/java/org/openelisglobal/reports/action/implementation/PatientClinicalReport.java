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
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.provider.validation.AlphanumAccessionValidator;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.internationalization.MessageUtil;
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

public class PatientClinicalReport extends PatientReport implements IReportCreator, IReportParameterSetter {

    private static Set<Integer> analysisStatusIds;
    private boolean isLNSP = false;
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

    public PatientClinicalReport() {
        super();
    }

    public PatientClinicalReport(boolean isLNSP) {
        super();
        this.isLNSP = isLNSP;
    }

    @Override
    protected String reportFileName() {
        return "PatientClinicalReport";
    }

    @Override
    protected void createReportItems() {
        List<Analysis> analysisList = analysisService
                .getAnalysesBySampleIdAndStatusId(sampleService.getId(currentSample), analysisStatusIds);

        List<Analysis> filteredAnalysisList = userService.filterAnalysesByLabUnitRoles(systemUserId, analysisList,
                Constants.ROLE_REPORTS);
        currentConclusion = null;
        Set<SampleItem> sampleSet = new HashSet<>();
        List<ClinicalPatientData> currentSampleReportItems = new ArrayList<>(filteredAnalysisList.size());
        for (Analysis analysis : filteredAnalysisList) {
            if (!analysis.getTest().isInLabOnly()) {
                sampleSet.add(analysis.getSampleItem());
                boolean hasParentResult = analysis.getParentResult() != null;

                if (analysis.getTest() != null) {
                    currentAnalysis = analysis;
                    ClinicalPatientData resultsData = buildClinicalPatientData(hasParentResult);

                    if (currentAnalysis.isReferredOut()) {
                        Referral referral = referralService.getReferralByAnalysisId(currentAnalysis.getId());
                        if (referral != null) {
                            List<ClinicalPatientData> referredData = addReferredTests(referral, resultsData);
                            currentSampleReportItems.addAll(referredData);
                        }
                    } else {
                        currentSampleReportItems.add(resultsData);
                    }
                }
            }
        }

        reportItems.addAll(currentSampleReportItems);
        setCollectionTime(sampleSet, currentSampleReportItems, false);
    }

    private List<ClinicalPatientData> addReferredTests(Referral referral, ClinicalPatientData parentData) {

        List<ClinicalPatientData> currentSampleReportItems = new ArrayList<>();
        List<ReferralResult> referralResults = referralResultService.getReferralResultsForReferral(referral.getId());
        NoteService noteService = SpringContext.getBean(NoteService.class);
        String note = noteService.getNotesAsString(currentAnalysis, false, true, "<br/>", FILTER, true);

        if (!referralResults.isEmpty()) {

            boolean referralTestAssigned = false;
            for (ReferralResult referralResult : referralResults) {
                if (referralResult.getTestId() != null) {
                    referralTestAssigned = true;
                }
            }

            if (!referralTestAssigned) {
                currentSampleReportItems.add(parentData);
            }
        } else {
            currentSampleReportItems.add(parentData);
        }

        for (int i = 0; i < referralResults.size(); i++) {
            if (referralResults.get(i).getResult() == null) {
                sampleCompleteMap.put(sampleService.getAccessionNumber(currentSample), Boolean.FALSE);
            } else {

                i = lastUsedReportReferralResultValue(referralResults, i);
                ReferralResult referralResult = referralResults.get(i);

                ClinicalPatientData data = new ClinicalPatientData();
                copyParentData(data, parentData);

                data.setResult(reportReferralResultValue);
                data.setNote(note);
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
                    data.setSectionSortOrder(analysisService.getTestSection(currentAnalysis).getSortOrderInt());
                    data.setTestSection(analysisService.getTestSection(currentAnalysis).getLocalizedName());
                }

                if (GenericValidator.isBlankOrNull(reportReferralResultValue)) {
                    sampleCompleteMap.put(sampleService.getAccessionNumber(currentSample), Boolean.FALSE);
                    data.setResult(MessageUtil.getMessage("report.test.status.inProgress"));
                } else {
                    data.setResult(reportReferralResultValue);
                }

                data.setAlerts(getResultFlag(referralResult.getResult(), null));
                data.setHasRangeAndUOM(
                        referralResult.getResult() != null && "N".equals(referralResult.getResult().getResultType()));

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

                if (o1.getSectionSortOrder() > o2.getSectionSortOrder()) {
                    return 1;
                } else if (o1.getSectionSortOrder() < o2.getSectionSortOrder()) {
                    return -1;
                }

                int o1Panel = Integer.MAX_VALUE;
                int o2Panel = Integer.MAX_VALUE;
                if (o1.getPanel() != null) {
                    o1Panel = o1.getPanel().getSortOrderInt();
                }
                if (o2.getPanel() != null) {
                    o2Panel = o2.getPanel().getSortOrderInt();
                }

                int panelSort = o1Panel - o2Panel;

                if (panelSort != 0) {
                    return panelSort;
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

            if (AccessionFormat.ALPHANUM.toString()
                    .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.AccessionFormat))) {
                reportItem.setAccessionNumber(AlphanumAccessionValidator
                        .convertAlphaNumLabNumForDisplay(reportItem.getAccessionNumber().split("-")[0]));
            } else {
                reportItem.setAccessionNumber(reportItem.getAccessionNumber().split("-")[0]);
            }
            reportItem.setCompleteFlag(MessageUtil
                    .getMessage(sampleCompleteMap.get(reportItem.getAccessionNumber()) ? "report.status.complete"
                            : "report.status.partial"));
            if (reportItem.isCorrectedResult()) {
                // The report is French only
                if (reportItem.getNote() != null && reportItem.getNote().length() > 0) {
                    reportItem.setNote("Résultat corrigé<br/>" + reportItem.getNote());
                } else {
                    reportItem.setNote("Résultat corrigé");
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

    @Override
    protected String getHeaderName() {
        return "GeneralHeader.jasper";
    }
}
