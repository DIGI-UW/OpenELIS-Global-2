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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.result.action.util;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analysis.valueholder.ResultFile;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.ResultSaveService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.beanAdapters.ResultSaveBeanAdapter;
import org.openelisglobal.common.services.serviceBeans.ResultSaveBean;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.referral.action.beanitems.ReferralItem;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralResult;
import org.openelisglobal.referral.valueholder.ReferralSet;
import org.openelisglobal.referral.valueholder.ReferralStatus;
import org.openelisglobal.result.service.ResultInventoryService;
import org.openelisglobal.result.service.ResultSignatureService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultInventory;
import org.openelisglobal.result.valueholder.ResultSignature;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.testanalyte.service.TestAnalyteService;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.openelisglobal.userrole.service.UserRoleService;

// TODO unused
public class ResultUtil {
    private static final DictionaryService dictionaryService = SpringContext.getBean(DictionaryService.class);
    private static final TestAnalyteService testAnalyteService = SpringContext.getBean(TestAnalyteService.class);
    private static final AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
    private static final NoteService noteService = SpringContext.getBean(NoteService.class);
    private static final MethodService methodService = SpringContext.getBean(MethodService.class);
    private static final UserModuleService userModuleService = SpringContext.getBean(UserModuleService.class);
    private static final SampleHumanService sampleHumanService = SpringContext.getBean(SampleHumanService.class);
    private static final UserRoleService userRoleService = SpringContext.getBean(UserRoleService.class);
    private static final ResultLimitService resultLimitService = SpringContext.getBean(ResultLimitService.class);
    private static final OrganizationService organizationService = SpringContext.getBean(OrganizationService.class);
    private static final ResultInventoryService resultInventoryService = SpringContext
            .getBean(ResultInventoryService.class);
    private static final ResultSignatureService resultSigService = SpringContext.getBean(ResultSignatureService.class);

    private static String RESULT_EDIT_ROLE_ID;
    private static String REFERRAL_CONFORMATION_ID;

    private static final String RESULT_SUBJECT = "Result Note";

    public static String getStringValueOfResult(Result result) {
        if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(result.getResultType())) {
            return dictionaryService.getDictionaryById(result.getValue()).getLocalizedName();
        } else {
            return result.getValue();
        }
    }

    @SuppressWarnings("unchecked")
    public static TestAnalyte getTestAnalyteForResult(Result result) {
        /*
         * The logic behind this code is that there is a matching of some analytes to
         * the number of times the test has been run. i.e. if there is a positive HIV
         * test some labs will run it again as a reflex. This code below is to make sure
         * that we don't have an endless loop, but it does not feel very robust. This is
         * due for a refactoring.
         *
         */
        if (result.getTestResult() != null) {
            List<TestAnalyte> testAnalyteList = testAnalyteService
                    .getAllTestAnalytesPerTest(result.getTestResult().getTest());

            if (testAnalyteList.size() == 1) {
                return testAnalyteList.get(0);
            }

            if (testAnalyteList.size() > 1) {
                int distanceFromRoot = 0;

                Analysis parentAnalysis = result.getAnalysis().getParentAnalysis();

                while (parentAnalysis != null) {
                    distanceFromRoot++;
                    parentAnalysis = parentAnalysis.getParentAnalysis();
                }

                int index = Math.min(distanceFromRoot, testAnalyteList.size() - 1);

                return testAnalyteList.get(index);
            }
        }
        return null;
    }

    /*
     * The logic behind this code is that there are some other matching analytes for
     * a specific result other than the Analyte fetched by getTestAnalyteForResult
     * which is set to a Result
     */
    @SuppressWarnings("unchecked")
    public static List<Analyte> getOtherAnalyteForResult(Result result) {
        List<Analyte> otherAnalyte = new ArrayList<>();
        if (result.getTestResult() != null) {
            List<TestAnalyte> testAnalyteList = testAnalyteService
                    .getAllTestAnalytesPerTest(result.getTestResult().getTest());
            if (testAnalyteList == null) {
                return otherAnalyte; // or handle the null case appropriately
            }

            Set<TestAnalyte> othertestAnalyteList = new HashSet<>();
            Analyte defaultAnalyte = result.getAnalyte();
            if (defaultAnalyte == null) {
                return otherAnalyte;
            }
            if (testAnalyteList.size() == 1) {
                return otherAnalyte;
            }

            if (testAnalyteList.size() > 1) {

                Analysis parentAnalysis = result.getAnalysis().getParentAnalysis();

                if (parentAnalysis == null) {
                    testAnalyteList.forEach(ta -> {
                        if (!ta.getAnalyte().getId().equals(defaultAnalyte.getId())) {
                            othertestAnalyteList.add(ta);
                        }
                    });
                }
                othertestAnalyteList.forEach(a -> {
                    otherAnalyte.add(a.getAnalyte());
                });

                return otherAnalyte;
            }
        }
        return otherAnalyte;
    }

    public static boolean areNotes(TestResultItem item) {
        return !GenericValidator.isBlankOrNull(item.getNote());
    }

    public static boolean isReferred(TestResultItem testResultItem) {
        // return testResultItem.isShadowReferredOut();
        return testResultItem.isRefer();
    }

    public static boolean isRejected(TestResultItem testResultItem) {
        return testResultItem.isShadowRejected();
    }

    public static boolean areResults(TestResultItem item) {
        return !(GenericValidator.isBlankOrNull(item.getShadowResultValue())
                || (TypeOfTestResultServiceImpl.ResultType.DICTIONARY.matches(item.getResultType())
                        && "0".equals(item.getShadowResultValue())))
                || (TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(item.getResultType())
                        && !GenericValidator.isBlankOrNull(item.getMultiSelectResultValues()));
    }

    public static boolean isForcedToAcceptance(TestResultItem item) {
        return !GenericValidator.isBlankOrNull(item.getForceTechApproval());
    }

    public static void createAnalysisOnlyUpdates(ResultsUpdateDataSet actionDataSet, HttpServletRequest request) {
        for (TestResultItem testResultItem : actionDataSet.getAnalysisOnlyChangeResults()) {

            Analysis analysis = analysisService.get(testResultItem.getAnalysisId());
            analysis.setSysUserId(ControllerUtills.getSysUserId(request));
            analysis.setCompletedDate(DateUtil.convertStringDateToSqlDate(testResultItem.getTestDate()));
            if (testResultItem.getAnalysisMethod() != null) {
                analysis.setAnalysisType(testResultItem.getAnalysisMethod());
            }
            if (!GenericValidator.isBlankOrNull(testResultItem.getTestMethod())) {
                analysis.setMethod(methodService.get(testResultItem.getTestMethod()));
            }
            if (testResultItem.getResultFile() != null) {
                ResultFile resultFile = createResultFile(testResultItem.getResultFile());
                if (resultFile != null) {
                    analysis.setResultFile(resultFile);
                }
            }
            actionDataSet.getModifiedAnalysis().add(analysis);
        }
    }

    public static void createResultsFromItems(ResultsUpdateDataSet actionDataSet, boolean supportReferrals,
            boolean alwaysValidate, boolean useTechnicianName, String statusRuleSet, HttpServletRequest request) {

        for (TestResultItem testResultItem : actionDataSet.getModifiedItems()) {

            Analysis analysis = analysisService.get(testResultItem.getAnalysisId());
            analysis.setStatusId(getStatusForTestResult(testResultItem, alwaysValidate));
            analysis.setSysUserId(ControllerUtills.getSysUserId(request));
            if (!GenericValidator.isBlankOrNull(testResultItem.getTestMethod())) {
                analysis.setMethod(methodService.get(testResultItem.getTestMethod()));
            }
            actionDataSet.getModifiedAnalysis().add(analysis);

            actionDataSet.addToNoteList(noteService.createSavableNote(analysis, NoteType.INTERNAL,
                    testResultItem.getNote(), RESULT_SUBJECT, ControllerUtills.getSysUserId(request)));

            if (testResultItem.isShadowRejected()) {
                testResultItem.setResultValue("");
                testResultItem.setShadowResultValue("");
                String rejectedReasonId = testResultItem.getRejectReasonId();
                for (IdValuePair rejectReason : DisplayListService.getInstance().getList(ListType.REJECTION_REASONS)) {
                    if (rejectedReasonId.equals(rejectReason.getId())) {
                        actionDataSet.addToNoteList(noteService.createSavableNote(analysis, NoteType.REJECTION_REASON,
                                rejectReason.getValue(), RESULT_SUBJECT, ControllerUtills.getSysUserId(request)));
                        break;
                    }
                }
            }

            ResultSaveBean bean = ResultSaveBeanAdapter.fromTestResultItem(testResultItem);
            ResultSaveService resultSaveService = new ResultSaveService(analysis,
                    ControllerUtills.getSysUserId(request));
            // deletable Results will be written to, not read
            List<Result> results = resultSaveService.createResultsFromTestResultItem(bean,
                    actionDataSet.getDeletableResults());

            analysis.setCorrectedSincePatientReport(
                    resultSaveService.isUpdatedResult() && analysisService.patientReportHasBeenDone(analysis));

            if (analysisService.hasBeenCorrectedSinceLastPatientReport(analysis)) {
                Note note = noteService.createSavableNote(analysis, NoteType.EXTERNAL,
                        MessageUtil.getMessage("note.corrected.result"), RESULT_SUBJECT,
                        ControllerUtills.getSysUserId(request));
                if (!noteService.duplicateNoteExists(note)) {
                    actionDataSet.addToNoteList(noteService.createSavableNote(analysis, NoteType.EXTERNAL,
                            MessageUtil.getMessage("note.corrected.result"), RESULT_SUBJECT,
                            ControllerUtills.getSysUserId(request)));
                }
            }

            // If there is more than one result then each user selected reflex gets mapped
            // to that result
            for (Result result : results) {
                addResult(result, testResultItem, analysis, results.size() > 1, actionDataSet, useTechnicianName,
                        request);

                if (analysisShouldBeUpdated(testResultItem, result, supportReferrals)) {
                    updateAnalysis(testResultItem, testResultItem.getTestDate(), analysis, statusRuleSet);
                }
            }
            if (supportReferrals && testResultItem.isRefer()) {
                handleReferrals(testResultItem, testResultItem.getReferralItem(), results, analysis, actionDataSet,
                        request);
            }
        }
    }

    public static void handleReferrals(TestResultItem testResultItem, ReferralItem referralItem, List<Result> results,
            Analysis analysis, ResultsUpdateDataSet actionDataSet, HttpServletRequest request) {
        // List<Referral> referrals = new ArrayList<>();
        Referral referral = new Referral();
        referral.setFhirUuid(UUID.randomUUID());
        referral.setStatus(ReferralStatus.SENT);
        referral.setSysUserId(actionDataSet.getCurrentUserId());
        referral.setReferralTypeId(REFERRAL_CONFORMATION_ID);
        referral.setRequesterName(testResultItem.getTechnician());

        referral.setRequestDate(new Timestamp(new Date().getTime()));
        referral.setSentDate(DateUtil.convertStringDateToTruncatedTimestamp(referralItem.getReferredSendDate()));
        referral.setRequesterName(referralItem.getReferrer());
        referral.setOrganization(organizationService.get(referralItem.getReferredInstituteId()));
        referral.setAnalysis(analysis);

        referral.setReferralReasonId(referralItem.getReferralReasonId());

        ReferralResult referralResult = new ReferralResult();
        referralResult.setReferralId(referral.getId());
        referralResult.setSysUserId(actionDataSet.getCurrentUserId());
        referralResult.setTestId(referralItem.getReferredTestId());
        if (results.size() == 1) {
            referralResult.setResult(results.get(0));
        }

        ReferralSet referralSet = new ReferralSet();
        referralSet.setReferral(referral);
        referralSet.getExistingReferralResults().add(referralResult);
        actionDataSet.getSavableReferralSets().add(referralSet);

        String originalResultNote = MessageUtil.getMessage("referral.original.result") + ": ";
        if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(testResultItem.getResultType())
                || TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(testResultItem.getResultType())) {
            if ("0".equals(testResultItem.getResultValue()) || StringUtils.isBlank(testResultItem.getResultValue())) {
                originalResultNote = originalResultNote + "";
            } else {
                Dictionary dictionary = dictionaryService.get(testResultItem.getResultValue());
                if (dictionary.getLocalizedDictionaryName() == null) {
                    originalResultNote = originalResultNote + dictionary.getDictEntry();
                } else {
                    originalResultNote = originalResultNote
                            + dictionary.getLocalizedDictionaryName().getLocalizedValue();
                }
            }
        } else {
            originalResultNote = originalResultNote + testResultItem.getResultValue();
        }

        actionDataSet.addToNoteList(noteService.createSavableNote(analysis, NoteType.INTERNAL, originalResultNote,
                RESULT_SUBJECT, ControllerUtills.getSysUserId(request)));
    }

    public static boolean analysisShouldBeUpdated(TestResultItem testResultItem, Result result,
            boolean supportReferrals) {
        return result != null && !GenericValidator.isBlankOrNull(result.getValue())
                || (supportReferrals && ResultUtil.isReferred(testResultItem))
                || ResultUtil.isForcedToAcceptance(testResultItem) || testResultItem.isShadowRejected();
    }

    public static void addResult(Result result, TestResultItem testResultItem, Analysis analysis,
            boolean multipleResultsForAnalysis, ResultsUpdateDataSet actionDataSet, boolean useTechnicianName,
            HttpServletRequest request) {
        boolean newResult = result.getId() == null;
        boolean newAnalysisInLoop = analysis != actionDataSet.getPreviousAnalysis();

        ResultSignature technicianResultSignature = null;

        if (useTechnicianName && newAnalysisInLoop) {
            technicianResultSignature = createTechnicianSignatureFromResultItem(testResultItem, request);
        }

        ResultInventory testKit = createTestKitLinkIfNeeded(testResultItem, ResultsLoadUtility.TESTKIT, request);
        if (testResultItem.getResultFile() != null) {
            ResultFile resultFile = createResultFile(testResultItem.getResultFile());
            if (resultFile != null) {
                analysis.setResultFile(resultFile);
            }
        }

        analysis.setReferredOut(testResultItem.isReferredOut());
        analysis.setEnteredDate(DateUtil.getNowAsTimestamp());

        if (newResult) {
            analysis.setEnteredDate(DateUtil.getNowAsTimestamp());
            analysis.setRevision("1");
        } else if (newAnalysisInLoop) {
            analysis.setRevision(String.valueOf(Integer.parseInt(analysis.getRevision()) + 1));
        }

        SampleService sampleService = SpringContext.getBean(SampleService.class);
        Sample sample = sampleService.getSampleByAccessionNumber(testResultItem.getAccessionNumber());
        Patient patient = sampleService.getPatient(sample);

        Map<String, List<String>> triggersToReflexesMap = new HashMap<>();

        getSelectedReflexes(testResultItem.getReflexJSONResult(), triggersToReflexesMap);

        if (newResult) {
            actionDataSet.getNewResults().add(new ResultSet(result, technicianResultSignature, testKit, patient, sample,
                    triggersToReflexesMap, multipleResultsForAnalysis));
        } else {
            actionDataSet.getModifiedResults().add(new ResultSet(result, technicianResultSignature, testKit, patient,
                    sample, triggersToReflexesMap, multipleResultsForAnalysis));
        }

        actionDataSet.setPreviousAnalysis(analysis);
    }

    public static String getStatusForTestResult(TestResultItem testResult, boolean alwaysValidate) {
        if (testResult.isShadowRejected() && ConfigurationProperties.getInstance()
                .isPropertyValueEqual(Property.VALIDATE_REJECTED_TESTS, "true")) {
            return SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalRejected);
        } else if (testResult.isShadowRejected()) {
            return SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled);
        } else if (alwaysValidate || !testResult.isValid() || ResultUtil.isForcedToAcceptance(testResult)) {
            return SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance);
        } else if (noResults(testResult.getShadowResultValue(), testResult.getMultiSelectResultValues(),
                testResult.getResultType())) {
            return SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted);
        } else {
            if (!GenericValidator.isBlankOrNull(testResult.getResultLimitId())) {
                ResultLimit resultLimit = resultLimitService.get(testResult.getResultLimitId());
                if (resultLimit.isAlwaysValidate()) {
                    return SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance);
                }
                if (TypeOfTestResultServiceImpl.ResultType.DICTIONARY.matches(testResult.getResultType())
                        && !testResult.getResultValue().equals(resultLimit.getDictionaryNormalId())) {
                    return SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance);
                }
            }

            return SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized);
        }
    }

    public static void getSelectedReflexes(String reflexJSONResult, Map<String, List<String>> triggersToReflexesMap) {
        if (!GenericValidator.isBlankOrNull(reflexJSONResult)) {
            JSONParser parser = new JSONParser();
            try {
                JSONObject jsonResult = (JSONObject) parser.parse(reflexJSONResult.replaceAll("'", "\""));

                for (Object compoundReflexes : jsonResult.values()) {
                    if (compoundReflexes != null) {
                        String triggerIds = (String) ((JSONObject) compoundReflexes).get("triggerIds");
                        List<String> selectedReflexIds = new ArrayList<>();
                        JSONArray selectedReflexes = (JSONArray) ((JSONObject) compoundReflexes).get("selected");
                        for (Object selectedReflex : selectedReflexes) {
                            selectedReflexIds.add(((String) selectedReflex));
                        }
                        triggersToReflexesMap.put(triggerIds.trim(), selectedReflexIds);
                    }
                }
            } catch (ParseException e) {
                LogEvent.logDebug(e);
            }
        }
    }

    public static boolean noResults(String value, String multiSelectValue, String type) {

        return (GenericValidator.isBlankOrNull(value) && GenericValidator.isBlankOrNull(multiSelectValue))
                || (TypeOfTestResultServiceImpl.ResultType.DICTIONARY.matches(type) && "0".equals(value));
    }

    public static ResultInventory createTestKitLinkIfNeeded(TestResultItem testResult, String testKitName,
            HttpServletRequest request) {
        ResultInventory testKit = null;

        if ((TestResultItem.ResultDisplayType.SYPHILIS.toString() == testResult.getResultDisplayType()
                || TestResultItem.ResultDisplayType.HIV.toString() == testResult.getResultDisplayType())
                && ResultsLoadUtility.TESTKIT.equals(testKitName)) {

            testKit = createTestKit(testResult, testKitName, testResult.getTestKitId(), request);
        }

        return testKit;
    }

    public static ResultInventory createTestKit(TestResultItem testResult, String testKitName, String testKitId,
            HttpServletRequest request) throws LIMSRuntimeException {
        ResultInventory testKit;
        testKit = new ResultInventory();

        if (!GenericValidator.isBlankOrNull(testKitId)) {
            testKit.setId(testKitId);
            testKit = resultInventoryService.get(testKitId);
        }

        testKit.setInventoryLocationId(testResult.getTestKitInventoryId());
        testKit.setDescription(testKitName);
        testKit.setSysUserId(ControllerUtills.getSysUserId(request));
        return testKit;
    }

    public static void updateAnalysis(TestResultItem testResultItem, String testDate, Analysis analysis,
            String statusRuleSet) {
        if (testResultItem.getAnalysisMethod() != null) {
            analysis.setAnalysisType(testResultItem.getAnalysisMethod());
        }
        // analysis.setStartedDateForDisplay(testDate);

        // This needs to be refactored -- part of the logic is in
        // getStatusForTestResult. RetroCI over rides to whatever was set before
        if (statusRuleSet.equals(IActionConstants.STATUS_RULES_RETROCI)) {
            if (!SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled)
                    .equals(analysis.getStatusId())) {
                analysis.setCompletedDate(DateUtil.convertStringDateToSqlDate(testDate));
                analysis.setStatusId(
                        SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance));
            }
        } else if (SpringContext.getBean(IStatusService.class).matches(analysis.getStatusId(), AnalysisStatus.Finalized)
                || SpringContext.getBean(IStatusService.class).matches(analysis.getStatusId(),
                        AnalysisStatus.TechnicalAcceptance)
                || (analysis.isReferredOut()
                        && !GenericValidator.isBlankOrNull(testResultItem.getShadowResultValue()))) {
            analysis.setCompletedDate(DateUtil.convertStringDateToSqlDate(testDate));
        }
    }

    public static ResultSignature createTechnicianSignatureFromResultItem(TestResultItem testResult,
            HttpServletRequest requset) {
        ResultSignature sig = null;

        // The technician signature may be blank if the user changed a
        // conclusion and then changed it back. It will be dirty
        // but will not need a signature
        if (!GenericValidator.isBlankOrNull(testResult.getTechnician())) {
            sig = new ResultSignature();

            if (!GenericValidator.isBlankOrNull(testResult.getTechnicianSignatureId())) {
                sig = resultSigService.get(testResult.getTechnicianSignatureId());
            }

            sig.setIsSupervisor(false);
            sig.setNonUserName(testResult.getTechnician());

            sig.setSysUserId(ControllerUtills.getSysUserId(requset));
        }
        return sig;
    }

    public static boolean modifyResultsRoleBased() {
        return "true"
                .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.roleRequiredForModifyResults));
    }

    public static boolean userNotInRole(HttpServletRequest request) {
        if (userModuleService.isUserAdmin(request)) {
            return false;
        }
        List<String> roleIds = userRoleService.getRoleIdsForUser(ControllerUtills.getSysUserId(request));
        return !roleIds.contains(RESULT_EDIT_ROLE_ID);
    }

    public static Patient getPatient(Sample sample) {
        return sampleHumanService.getPatientForSample(sample);
    }

    public static ResultFile createResultFile(TestResultItem.ResultFileForm fileForm) {
        if (fileForm == null || GenericValidator.isBlankOrNull(fileForm.getFileName())
                || GenericValidator.isBlankOrNull(fileForm.getFileType()) || fileForm.getContent() == null
                || fileForm.getContent().length == 0) {
            return null;
        }
        ResultFile file = new ResultFile();
        file.setFileName(fileForm.getFileName());
        file.setFileType(fileForm.getFileType());
        file.setContent(fileForm.getContent());

        Timestamp now = new Timestamp(System.currentTimeMillis());
        file.setUploadedAt(now);
        file.setLastupdated(now);

        return file;
    }

}
