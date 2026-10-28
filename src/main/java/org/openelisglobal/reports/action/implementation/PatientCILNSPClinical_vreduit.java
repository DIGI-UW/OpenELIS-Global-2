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

import java.io.ByteArrayInputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.accreditation.dto.AccreditationReportData;
import org.openelisglobal.accreditation.service.AccreditationReportService;
import org.openelisglobal.analysis.service.AnalysisServiceImpl;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.provider.validation.AlphanumAccessionValidator;
import org.openelisglobal.common.services.IReportTrackingService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.ReportTrackingService.ReportType;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.image.service.ImageService;
import org.openelisglobal.image.valueholder.Image;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.internationalization.UiMessages;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.qaevent.criticalcallback.service.CriticalCallbackService;
import org.openelisglobal.qaevent.criticalcallback.valueholder.CriticalCallback;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralResult;
import org.openelisglobal.reports.action.implementation.reportBeans.ClinicalPatientData;
import org.openelisglobal.reports.action.implementation.reportBeans.PatientReportOrder;
import org.openelisglobal.reports.action.implementation.reportBeans.ReportCallbackLine;
import org.openelisglobal.reports.action.implementation.reportBeans.ReportSampleLine;
import org.openelisglobal.reports.valueholder.DocumentTrack;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * The routine patient report (Reports › Patient Status Report): one report per
 * order with its Samples block, its results by section and its sign-off, drawn
 * by {@link PatientResultsPdf}. Besides each result row it gathers what the
 * report prints once per order (FRS OGC-1111 Part A).
 */
public class PatientCILNSPClinical_vreduit extends PatientReport implements IReportCreator, IReportParameterSetter {

    /** The report name the menu requests, under which each print is tracked. */
    public static final String REPORT_NAME = "patientCILNSP_vreduit";

    private final Set<String> analysisStatusIds = new HashSet<>();
    private final Set<String> validatedAnalysisStatusIds = new HashSet<>();
    protected List<ClinicalPatientData> clinicalReportItems;
    private ImageService imageService = SpringContext.getBean(ImageService.class);
    private Map<String, PatientReportOrder> reportOrders;
    private Map<String, OrderWork> orderWork;
    private UiMessages messages;

    static final String configName = ConfigurationProperties.getInstance().getPropertyValue(Property.configurationName);

    /**
     * What one order gathers while its rows are built, resolved once they all are.
     */
    private static final class OrderWork {
        private final List<Analysis> analyses = new ArrayList<>();
        private final Set<String> claimableTestIds = new HashSet<>();
        private LocalDate releaseDay;
        private boolean pending;
    }

    public PatientCILNSPClinical_vreduit() {
        super();
        IStatusService statusService = SpringContext.getBean(IStatusService.class);
        // Include samples rejected at order entry so they appear on the report
        // rather than being silently omitted (OGC #3).
        for (AnalysisStatus status : new AnalysisStatus[] { AnalysisStatus.BiologistRejected, AnalysisStatus.Finalized,
                AnalysisStatus.NonConforming_depricated, AnalysisStatus.NotStarted, AnalysisStatus.TechnicalAcceptance,
                AnalysisStatus.Canceled, AnalysisStatus.TechnicalRejected, AnalysisStatus.SampleRejected }) {
            analysisStatusIds.add(statusService.getStatusID(status));
        }
        validatedAnalysisStatusIds.add(statusService.getStatusID(AnalysisStatus.Finalized));
    }

    @Override
    protected void createReportParameters() {
        super.createReportParameters();
        reportParameters.put("billingNumberLabel",
                SpringContext.getBean(LocalizationService.class).getLocalizedValueById(ConfigurationProperties
                        .getInstance().getPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL)));
        reportParameters.put("footerName", getFooterName());
        Optional<Image> labDirectorSignature = imageService.getImageBySiteInfoName("labDirectorSignature");
        reportParameters.put("useLabDirectorSignature", labDirectorSignature.isPresent());
        if (labDirectorSignature.isPresent()) {
            reportParameters.put("labDirectorSignature",
                    new ByteArrayInputStream(labDirectorSignature.get().getImage()));
        }

        reportParameters.put("labDirectorName",
                ConfigurationProperties.getInstance().getPropertyValue(Property.LAB_DIRECTOR_NAME));
        reportParameters.put("labDirectorTitle",
                ConfigurationProperties.getInstance().getPropertyValue(Property.LAB_DIRECTOR_TITLE));
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
        String orderKey = convertToAlphaNumericDisplay(currentSample);
        OrderWork work = orderWork.computeIfAbsent(orderKey, key -> new OrderWork());
        Set<SampleItem> sampleSet = new HashSet<>();

        boolean isConfirmationSample = sampleService.isConfirmationSample(currentSample);
        List<Analysis> analysisList = analysisService
                .getAnalysesBySampleIdAndStatusId(sampleService.getId(currentSample), analysisStatusIds);
        if (onlyResultsForReportBySite) {
            analysisList = analysisService.getAnalysesBySampleIdAndStatusId(sampleService.getId(currentSample),
                    validatedAnalysisStatusIds);
        }

        List<Analysis> filteredAnalysisList = filterAnalysesForReportUser(analysisList);
        List<ClinicalPatientData> currentSampleReportItems = new ArrayList<>(filteredAnalysisList.size());
        currentConclusion = null;
        for (Analysis analysis : filteredAnalysisList) {
            if (!analysis.getTest().isInLabOnly()) {
                boolean hasParentResult = analysis.getParentResult() != null;
                sampleSet.add(analysis.getSampleItem());
                if (analysis.getTest() != null) {
                    currentAnalysis = analysis;
                    work.analyses.add(analysis);
                    ClinicalPatientData resultsData = buildClinicalPatientData(hasParentResult);
                    // Reflect the analysis's real status (in progress / waiting
                    // validation / validated / rejected) rather than always "in
                    // progress" and blank-for-validated.
                    resultsData.setAnalysisStatus(reportAnalysisStatus(currentAnalysis));
                    describeAnalysis(resultsData, analysis, work);
                    if (isConfirmationSample) {
                        String alerts = resultsData.getAlerts();
                        if (!GenericValidator.isBlankOrNull(alerts)) {
                            alerts += ", C";
                        } else {
                            alerts = "C";
                        }

                        resultsData.setAlerts(alerts);
                        resultsData.setConfirmationSample(true);
                    }

                    if (currentAnalysis.isReferredOut()) {
                        Referral referral = referralService.getReferralByAnalysisId(currentAnalysis.getId());
                        if (referral != null) {
                            // addReferredTests method in both PatientClinical and PatientCILNSPClinical are
                            // nearly identical and
                            // should be refactored to use the same code.
                            List<ClinicalPatientData> referredData = addReferredTests(referral, resultsData, work);
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
        sampleCompleteMap.put(orderKey, !work.pending);
        reportOrders.put(orderKey, describeOrder(work));
    }

    /**
     * The row fields the report reads besides the result: what the analysis is,
     * when it was released, its method and the sample number it prints under.
     */
    private void describeAnalysis(ClinicalPatientData data, Analysis analysis, OrderWork work) {
        IStatusService statusService = SpringContext.getBean(IStatusService.class);
        String statusId = analysisService.getStatusId(analysis);
        data.setAnalysisId(analysis.getId());
        data.setTestId(analysis.getTest().getId());
        if (analysis.getMethod() != null) {
            data.setMethodName(analysis.getMethod().getLocalizedValue());
        }
        data.setSampleSortOrder(primarySampleItem(analysis.getSampleItem()).getSortOrder());
        boolean released = statusService.matches(statusId, AnalysisStatus.Finalized);
        data.setReleased(released);
        data.setSampleRejected(statusService.matches(statusId, AnalysisStatus.SampleRejected));
        data.setTestRejected(statusService.matches(statusId, AnalysisStatus.TechnicalRejected)
                || statusService.matches(statusId, AnalysisStatus.BiologistRejected));
        if (statusService.matches(statusId, AnalysisStatus.Canceled)) {
            data.setResult(messages.get("order.status.cancelled"));
        }
        if (released) {
            data.setCompletedTime(reportTime(analysis.getReleasedDate()));
            if (!analysis.isReferredOut()) {
                work.claimableTestIds.add(analysis.getTest().getId());
                if (analysis.getReleasedDate() != null) {
                    LocalDate day = analysis.getReleasedDate().toLocalDateTime().toLocalDate();
                    if (work.releaseDay == null || day.isAfter(work.releaseDay)) {
                        work.releaseDay = day;
                    }
                }
            }
        } else if (!data.isSampleRejected() && !data.isTestRejected()
                && !statusService.matches(statusId, AnalysisStatus.Canceled)) {
            work.pending = true;
        }
    }

    /**
     * The order date as the report prints it. A requested date is a calendar day
     * and prints without a time; an order without one falls back to its receipt.
     */
    private String orderDate(Timestamp received) {
        ObservationHistory requested = SpringContext.getBean(ObservationHistoryService.class)
                .getObservationForSample(ObservationType.REQUEST_DATE, sampleService.getId(currentSample));
        if (requested == null || GenericValidator.isBlankOrNull(requested.getValue())) {
            return reportTime(received);
        }
        return new SimpleDateFormat(DateUtil.getDateFormat().replace("yyyy", "yy"))
                .format(DateUtil.convertStringDateToTruncatedTimestamp(requested.getValue()));
    }

    /**
     * The sample an aliquot came from; a primary sample is its own. Each parent is
     * loaded by id: the parent reference is a lazy proxy, and a primary sample with
     * no test of its own is not otherwise loaded.
     */
    private static SampleItem primarySampleItem(SampleItem item) {
        SampleItemService sampleItemService = SpringContext.getBean(SampleItemService.class);
        SampleItem primary = item;
        while (primary.getParentSampleItem() != null) {
            primary = sampleItemService.get(primary.getParentSampleItem().getId());
        }
        return primary;
    }

    /**
     * The order's Samples block, completion, release summary and corrected-report
     * reference, from the analyses its rows were built from.
     */
    private PatientReportOrder describeOrder(OrderWork work) {
        PatientReportOrder order = new PatientReportOrder();
        order.setComplete(!work.pending);
        describeSamples(order, work);
        describeRelease(order, work);
        describeCorrection(order, work);
        return order;
    }

    private void describeSamples(PatientReportOrder order, OrderWork work) {
        Set<String> printedItemIds = new HashSet<>();
        for (Analysis analysis : work.analyses) {
            printedItemIds.add(primarySampleItem(analysis.getSampleItem()).getId());
        }
        List<SampleItem> items = new ArrayList<>(SpringContext.getBean(SampleItemService.class)
                .getSampleItemsBySampleId(sampleService.getId(currentSample)));
        items.removeIf(item -> item.isVoided() || item.getParentSampleItem() != null
                || !(printedItemIds.contains(item.getId()) || item.isRejected()));
        items.sort(Comparator.comparingInt(item -> sortNumber(item.getSortOrder())));
        Timestamp orderReceived = currentSample.getReceivedTimestamp();
        order.setOrderDate(orderDate(orderReceived));
        List<ReportSampleLine> lines = new ArrayList<>();
        Timestamp earliest = null;
        Set<Timestamp> collectionTimes = new HashSet<>();
        for (SampleItem item : items) {
            Timestamp collected = item.getCollectionDate();
            if (collected != null) {
                collectionTimes.add(collected);
                if (earliest == null || collected.before(earliest)) {
                    earliest = collected;
                }
            }
            Timestamp received = item.getReceivedDate() != null ? item.getReceivedDate() : orderReceived;
            String reason = item.isRejected() ? rejectionReason(item.getRejectReasonId()) : null;
            lines.add(new ReportSampleLine(item.getSortOrder(),
                    item.getTypeOfSample() == null ? "" : item.getTypeOfSample().getLocalizedName(),
                    reportTime(collected), reportTime(received), reason,
                    GenericValidator.isBlankOrNull(item.getCollectionConditions()) ? null
                            : item.getCollectionConditions()));
        }
        order.setSamples(lines);
        order.setEarliestCollection(reportTime(earliest));
        order.setCollectionsDiffer(collectionTimes.size() > 1);
    }

    private static int sortNumber(String sortOrder) {
        try {
            return Integer.parseInt(sortOrder);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    /** The rejection reason's localized name; blank when none was recorded. */
    private String rejectionReason(String reasonId) {
        if (GenericValidator.isBlankOrNull(reasonId)) {
            return "";
        }
        Dictionary reason = dictionaryService.getDictionaryById(reasonId);
        return reason == null || reason.getLocalizedName() == null ? "" : reason.getLocalizedName();
    }

    /**
     * Who validated and released the order's results, each with their latest
     * release time, and when the last result was released. Validation stamps the
     * release time on the analysis and records the validating user only in the
     * analysis's audit history, so each validator is the author of the first
     * history entry written at or after the release.
     */
    private void describeRelease(PatientReportOrder order, OrderWork work) {
        Map<String, Timestamp> latestByValidator = new LinkedHashMap<>();
        Timestamp lastRelease = null;
        for (Analysis analysis : work.analyses) {
            Timestamp released = analysis.getReleasedDate();
            if (released == null || !SpringContext.getBean(IStatusService.class)
                    .matches(analysisService.getStatusId(analysis), AnalysisStatus.Finalized)) {
                continue;
            }
            if (lastRelease == null || released.after(lastRelease)) {
                lastRelease = released;
            }
            String validator = validatorName(analysis);
            if (validator != null) {
                latestByValidator.merge(validator, released, (a, b) -> a.after(b) ? a : b);
            }
        }
        List<Map.Entry<String, Timestamp>> validators = new ArrayList<>(latestByValidator.entrySet());
        validators.sort(Map.Entry.comparingByValue());
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Timestamp> validator : validators) {
            lines.add(validator.getKey() + ", " + reportTime(validator.getValue()));
        }
        order.setValidators(lines);
        order.setCompletedTime(work.pending ? null : reportTime(lastRelease));
    }

    private String validatorName(Analysis analysis) {
        History release = null;
        for (History entry : SpringContext.getBean(HistoryService.class)
                .getHistoryByRefIdAndRefTableId(analysis.getId(), AnalysisServiceImpl.getTableReferenceId())) {
            if (entry.getTimestamp() != null && !entry.getTimestamp().before(analysis.getReleasedDate())
                    && (release == null || entry.getTimestamp().before(release.getTimestamp()))) {
                release = entry;
            }
        }
        if (release == null || GenericValidator.isBlankOrNull(release.getSysUserId())) {
            return null;
        }
        return userName(release.getSysUserId());
    }

    private static String userName(String systemUserId) {
        if (GenericValidator.isBlankOrNull(systemUserId)) {
            return null;
        }
        SystemUser user = SpringContext.getBean(SystemUserService.class).getUserById(systemUserId);
        return user == null ? null : user.getNameForDisplay();
    }

    /**
     * What the Report Print Queue decided before this render: the version number
     * the print creates, whether it amends a printed result, the recorded change
     * summary and when the replaced version was issued.
     */
    public static final class IssueContext {
        private final int version;
        private final boolean amended;
        private final String changeSummary;
        private final String replacedIssueTime;
        private final String issueTime;

        public IssueContext(int version, boolean amended, String changeSummary, String replacedIssueTime) {
            this(version, amended, changeSummary, replacedIssueTime, null);
        }

        /**
         * @param issueTime when this version was first issued, as
         *                  {@link #reportTime(Timestamp)} prints it; the footer shows
         *                  it instead of the render time (FRS OGC-1031 FR-4.6)
         */
        public IssueContext(int version, boolean amended, String changeSummary, String replacedIssueTime,
                String issueTime) {
            this.version = version;
            this.amended = amended;
            this.changeSummary = changeSummary;
            this.replacedIssueTime = replacedIssueTime;
            this.issueTime = issueTime;
        }
    }

    private IssueContext issueContext;

    /**
     * Set by the Report Print Queue pipeline; without it the corrected banner is
     * derived from the corrected-result notes and the print history, as before.
     */
    public void setIssueContext(IssueContext issueContext) {
        this.issueContext = issueContext;
    }

    /**
     * Whether a result on the order was corrected after its report was issued, and
     * when the report this one replaces was issued. Results Entry and Validation
     * mark a correction with the corrected-result note, which survives reprints, so
     * the banner is derived from that note rather than the one-time flag.
     */
    private void describeCorrection(PatientReportOrder order, OrderWork work) {
        if (issueContext != null) {
            order.setVersion(issueContext.version);
            order.setIssueTime(issueContext.issueTime);
            order.setChangeSummary(issueContext.changeSummary);
            order.setCorrected(issueContext.amended);
            order.setReplacedIssueTime(issueContext.replacedIssueTime);
            return;
        }
        Set<String> correctedTexts = new HashSet<>();
        for (Locale locale : List.of(Locale.ENGLISH, Locale.FRENCH, LocaleContextHolder.getLocale())) {
            correctedTexts.add(MessageUtil.getMessage("note.corrected.result", locale));
        }
        Timestamp correctedAt = null;
        boolean corrected = false;
        for (Analysis analysis : work.analyses) {
            if (analysis.isCorrectedSincePatientReport()) {
                corrected = true;
            }
            for (Note note : noteService.getNotesChronologicallyByRefIdAndRefTable(analysis.getId(),
                    AnalysisServiceImpl.getTableReferenceId())) {
                if (NoteType.EXTERNAL.getDBCode().equals(note.getNoteType())
                        && correctedTexts.contains(note.getText())) {
                    corrected = true;
                    if (note.getLastupdated() != null
                            && (correctedAt == null || note.getLastupdated().after(correctedAt))) {
                        correctedAt = note.getLastupdated();
                    }
                }
            }
        }
        order.setCorrected(corrected);
        if (!corrected) {
            return;
        }
        DocumentTrack replaced = null;
        for (DocumentTrack issue : SpringContext.getBean(IReportTrackingService.class)
                .getReportsForSampleAndReportName(currentSample, ReportType.PATIENT, REPORT_NAME)) {
            if (issue.getReportTime() == null || (correctedAt != null && !issue.getReportTime().before(correctedAt))) {
                continue;
            }
            if (replaced == null || issue.getReportTime().after(replaced.getReportTime())) {
                replaced = issue;
            }
        }
        order.setReplacedIssueTime(replaced == null ? null : reportTime(replaced.getReportTime()));
    }

    @Override
    protected void setEmptyResult(ClinicalPatientData data) {
        data.setAnalysisStatus(messages.get("dashboard.in.progress.label"));
    }

    /**
     * The status a row prints in its Result cell while the result is not released:
     * awaiting validation once results are entered, otherwise in progress. A
     * released, rejected or cancelled row prints its result or its rejection
     * instead.
     */
    private String reportAnalysisStatus(Analysis analysis) {
        IStatusService statusService = SpringContext.getBean(IStatusService.class);
        String statusId = analysisService.getStatusId(analysis);
        if (statusService.matches(statusId, AnalysisStatus.TechnicalAcceptance)) {
            return messages.get("report.patient.awaitingValidation");
        }
        return messages.get("dashboard.in.progress.label");
    }

    /** A component without a result reads as an unreleased test row does. */
    @Override
    protected String componentInProgressText() {
        return messages.get("dashboard.in.progress.label");
    }

    /** A component awaiting validation reads as an unreleased test row does. */
    @Override
    protected String componentAwaitingValidationText() {
        return messages.get("report.patient.awaitingValidation");
    }

    @Override
    protected void setReferredOutResult(ClinicalPatientData data) {
        data.setAlerts("R");
        data.setReferralPending(true);
        data.setAnalysisStatus(messages.get("dashboard.in.progress.label"));
    }

    // addReferredTests method in both PatientClinical and PatientCILNSPClinical are
    // nearly identical and
    // should be refactored to use the same code.
    private List<ClinicalPatientData> addReferredTests(Referral referral, ClinicalPatientData parentData,
            OrderWork work) {
        List<ReferralResult> referralResults = referralResultService.getReferralResultsForReferral(referral.getId());
        NoteService noteService = SpringContext.getBean(NoteService.class);
        String note = noteService.getNotesAsString(currentAnalysis, false, true, "<br/>", FILTER, true);
        String performingLab = referral.getOrganization() == null ? null
                : referral.getOrganization().getOrganizationName();
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
                work.pending = true;
            }
        } else {
            reportItems.add(parentData);
            currentSampleReportItems.add(parentData);
            work.pending = true;
        }
        for (int i = 0; i < referralResults.size(); i++) {
            if (referralResults.get(i).getResult() == null) {
                work.pending = true;
            } else {

                i = lastUsedReportReferralResultValue(referralResults, i);
                ReferralResult referralResult = referralResults.get(i);

                ClinicalPatientData data = new ClinicalPatientData();
                copyParentData(data, parentData);

                data.setResult(reportReferralResultValue);
                data.setNote(note);
                data.setSampleType(parentData.getSampleType());
                data.setSampleId(parentData.getSampleId());
                data.setSampleSortOrder(parentData.getSampleSortOrder());
                data.setAnalysisId(parentData.getAnalysisId());
                data.setConfirmationSample(parentData.isConfirmationSample());
                String testId = referralResult.getTestId();
                if (!GenericValidator.isBlankOrNull(testId)) {
                    Test test = new Test();
                    test.setId(testId);
                    testService.getData(test);
                    data.setTestId(testId);
                    data.setTestName(TestServiceImpl.getUserLocalizedReportingTestName(test,
                            appendSampleTypeToTestName() ? parentData.getSampleType() : null));

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
                    work.pending = true;
                    data.setReferralPending(true);
                    data.setAnalysisStatus(messages.get("dashboard.in.progress.label"));
                } else if (!parentData.isReleased()) {
                    data.setResult(null);
                    data.setPerformingLabName(performingLab);
                    data.setAnalysisStatus(parentData.getAnalysisStatus());
                } else {
                    data.setResult(reportReferralResultValue);
                    data.setPerformingLabName(performingLab);
                    data.setReleased(true);
                    data.setCompletedTime(parentData.getCompletedTime());
                }

                if (data.isReleased() || data.isReferralPending()) {
                    data.setAlerts(getResultFlag(referralResult.getResult(), null, data));
                }
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
        data.setSubjectNumber(parentData.getSubjectNumber());
        data.setBillingNumber(parentData.getBillingNumber());
        data.setLabOrderType(parentData.getLabOrderType());
        data.setOrderDate(parentData.getOrderDate());
        data.setContactTracingIndexName(parentData.getContactTracingIndexName());
        data.setContactTracingIndexRecordNumber(parentData.getContactTracingIndexRecordNumber());
        if (AccessionFormat.ALPHANUM.toString()
                .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.AccessionFormat))) {
            data.setAccessionNumber(
                    AlphanumAccessionValidator.convertAlphaNumLabNumForDisplay(parentData.getAccessionNumber()));
        } else {
            data.setAccessionNumber(parentData.getAccessionNumber());
        }
    }

    @Override
    protected void postSampleBuild() {
        if (reportItems.isEmpty()) {
            ClinicalPatientData reportItem = buildClinicalPatientData(false);
            reportItem.setTestSection(MessageUtil.getMessage("report.no.results"));
            clinicalReportItems.add(reportItem);
        } else {
            buildReport();
            addCallbacks();
            addOrderAccreditation();
        }
    }

    /**
     * The latest callback under each critical row, with the count of earlier ones.
     */
    void addCallbacks() {
        Set<String> criticalAnalysisIds = new HashSet<>();
        for (ClinicalPatientData item : reportItems) {
            if (isCritical(item.getFlagCode()) && item.getAnalysisId() != null) {
                criticalAnalysisIds.add(item.getAnalysisId());
            }
        }
        if (criticalAnalysisIds.isEmpty()) {
            return;
        }
        Map<String, List<CriticalCallback>> byAnalysis = new HashMap<>();
        for (CriticalCallback callback : SpringContext.getBean(CriticalCallbackService.class)
                .getCallbacksForAnalyses(criticalAnalysisIds)) {
            byAnalysis.computeIfAbsent(callback.getAnalysisId(), key -> new ArrayList<>()).add(callback);
        }
        for (ClinicalPatientData item : reportItems) {
            List<CriticalCallback> callbacks = isCritical(item.getFlagCode()) ? byAnalysis.get(item.getAnalysisId())
                    : null;
            if (callbacks == null || callbacks.isEmpty()) {
                continue;
            }
            CriticalCallback latest = callbacks.get(0);
            item.setCallback(new ReportCallbackLine(latest.getRecipientName(), reportTime(latest.getLoggedAt()),
                    userName(latest.getLoggedBy()), latest.getStatus(), callbacks.size() - 1));
        }
    }

    static boolean isCritical(String flagCode) {
        return ResultAlertFlags.CRITICALLY_ABOVE.equals(flagCode) || ResultAlertFlags.CRITICALLY_BELOW.equals(flagCode);
    }

    /**
     * Each order's accreditation marks, resolved from its own validated in-house
     * tests as of its own release date, and the rows outside the accredited scope
     * when a mark prints (FRS FR-A13, FR-A28).
     */
    private void addOrderAccreditation() {
        AccreditationReportService accreditation = SpringContext.getBean(AccreditationReportService.class);
        for (Map.Entry<String, PatientReportOrder> entry : reportOrders.entrySet()) {
            OrderWork work = orderWork.get(entry.getKey());
            PatientReportOrder order = entry.getValue();
            AccreditationReportData marks = accreditation.resolve(work.claimableTestIds, work.releaseDay);
            order.setAccreditationLogos(marks.getLogos());
            order.setAccreditingBodies(marks.getBodyNames());
            if (marks.getLogos().isEmpty()) {
                for (ClinicalPatientData item : reportItems) {
                    if (entry.getKey().equals(item.getAccessionNumber())) {
                        item.setInAccreditedScope(true);
                    }
                }
                continue;
            }
            Set<String> testIds = new HashSet<>();
            for (ClinicalPatientData item : reportItems) {
                if (entry.getKey().equals(item.getAccessionNumber()) && item.getTestId() != null) {
                    testIds.add(item.getTestId());
                }
            }
            Set<String> inScope = accreditation.testsInAccreditedScope(testIds, work.releaseDay);
            for (ClinicalPatientData item : reportItems) {
                if (entry.getKey().equals(item.getAccessionNumber())) {
                    item.setInAccreditedScope(item.getPerformingLabName() == null && !item.isReferralPending()
                            && inScope.contains(item.getTestId()));
                }
            }
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
            reportItem
                    .setCorrectedResult(sampleCorrectedMap.get(reportItem.getAccessionNumber().split("_")[0]) != null);
        }
    }

    @Override
    protected String getReportNameForParameterPage() {
        return MessageUtil.getMessage("openreports.patientTestStatus");
    }

    @Override
    protected byte[] renderReport() {
        byte[] signature = imageService.getImageBySiteInfoName("labDirectorSignature").map(Image::getImage)
                .orElse(null);
        return PatientResultsPdf.render(new PatientResultsPdf.Settings(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SiteName),
                ConfigurationProperties.getInstance().getPropertyValue(Property.ADDITIONAL_SITE_INFO),
                siteImage("headerLeftImage"), siteImage("headerRightImage"),
                Boolean.TRUE.equals(reportParameters.get("useBillingNumber")),
                (String) reportParameters.get("billingNumberLabel"),
                Boolean.TRUE.equals(reportParameters.get("useSTNumber")),
                Boolean.TRUE.equals(reportParameters.get("useContactTracing")), signature,
                (String) reportParameters.get("labDirectorName"), (String) reportParameters.get("labDirectorTitle")),
                messages, reportItems, reportOrders);
    }

    /** What each order prints once, by the rows' accession number. */
    Map<String, PatientReportOrder> getReportOrders() {
        return reportOrders;
    }

    private byte[] siteImage(String name) {
        return imageService.getImageBySiteInfoName(name).map(Image::getImage).orElse(null);
    }

    @Override
    protected void initializeReportItems() {
        super.initializeReportItems();
        clinicalReportItems = new ArrayList<>();
        reportOrders = new LinkedHashMap<>();
        orderWork = new HashMap<>();
        messages = UiMessages.current();
    }

    /**
     * A date and time as the report prints it: the lab's date and clock format with
     * a two-digit year and, on a 12-hour clock, the compact "1:05PM", which keeps
     * the Completed and Samples columns to one line.
     */
    public static String reportTime(Timestamp time) {
        if (time == null) {
            return null;
        }
        boolean clock24 = ConfigurationProperties.getInstance().isPropertyValueEqual(Property.CLOCK_24, "true");
        String pattern = clock24 ? DateUtil.getDateTimeFormat()
                : DateUtil.getDateTime12HourFormat().replace("KK:mm a", "h:mma");
        return new SimpleDateFormat(pattern.replace("yyyy", "yy")).format(time);
    }

    @Override
    protected void setReferredResult(ClinicalPatientData data, Result result) {
        data.setResult(data.getResult());
        data.setAlerts(getResultFlag(result, null, data));
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
    protected boolean appendSampleTypeToTestName() {
        return true;
    }
}
