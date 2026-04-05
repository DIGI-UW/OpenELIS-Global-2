package org.openelisglobal.result.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.BidirectionalAnalyzer;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.analyzerresults.action.AnalyzerResultsPaging;
import org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsAcceptService;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.formfields.FormFields.Field;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.paging.PagingBean.Paging;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.common.services.PluginMenuService;
import org.openelisglobal.common.services.QAService;
import org.openelisglobal.common.services.QAService.QAObservationType;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;
import org.openelisglobal.result.form.AnalyzerResultsForm;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testreflex.action.util.TestReflexUtil;
import org.openelisglobal.testreflex.service.TestReflexService;
import org.openelisglobal.testreflex.valueholder.TestReflex;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AnalyzerResultsController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "type", "paging.currentPage", "resultList*.id",
            "resultList*.sampleGroupingNumber", "resultList*.readOnly", "resultList*.testResultType",
            "resultList*.testId", "resultList*.accessionNumber", "resultList*.isAccepted", "resultList*.isRejected",
            "resultList*.isDeleted", "resultList*.result", "resultList*.completeDate", "resultList*.note",
            "resultList*.reflexSelectionId", };

    private static final boolean IS_RETROCI = ConfigurationProperties.getInstance()
            .isPropertyValueEqual(ConfigurationProperties.Property.configurationName, "CI_GENERAL");
    private static final String REJECT_VALUE = "XXXX";
    private String RESULT_SUBJECT = "Analyzer Result Note";

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private TestService testService;
    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;
    @Autowired
    private AnalyzerResultsService analyzerResultsService;
    @Autowired
    private AnalyzerResultsAcceptService acceptService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private TestResultService testResultService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private TypeOfSampleTestService sampleTypeTestService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private TestReflexService testReflexService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private SampleQaEventService sampleQaEventService;
    @Autowired
    private LocalizationService localizationService;
    @Autowired
    private NoteService noteService;
    @Autowired
    private PluginAnalyzerService pluginAnalyzerService;
    @Autowired
    private AnalyzerService analyzerService;

    // used in constructor, so use constructor injection
    private TypeOfSampleService typeOfSampleService;

    private TestReflexUtil reflexUtil = new TestReflexUtil();

    private Map<String, String> analyzerNameToSubtitleKey = new HashMap<>();
    private final String DBS_SAMPLE_TYPE_ID;

    public AnalyzerResultsController(TypeOfSampleService typeOfSampleService) {
        this.typeOfSampleService = typeOfSampleService;

        if (IS_RETROCI) {
            TypeOfSample typeOfSample = new TypeOfSample();
            typeOfSample.setDescription("DBS");
            typeOfSample.setDomain("H");
            typeOfSample = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(typeOfSample, false);
            DBS_SAMPLE_TYPE_ID = typeOfSample.getId();
        } else {
            DBS_SAMPLE_TYPE_ID = null;
        }

        analyzerNameToSubtitleKey.put(AnalyzerTestNameCache.COBAS_INTEGRA400_NAME, "banner.menu.results.cobas.integra");
        analyzerNameToSubtitleKey.put(AnalyzerTestNameCache.SYSMEX_XT2000_NAME, "banner.menu.results.sysmex");
        analyzerNameToSubtitleKey.put(AnalyzerTestNameCache.FACSCALIBUR, "banner.menu.results.facscalibur");
        analyzerNameToSubtitleKey.put(AnalyzerTestNameCache.FACSCANTO, "banner.menu.results.facscanto");
        analyzerNameToSubtitleKey.put(AnalyzerTestNameCache.EVOLIS, "banner.menu.results.evolis");
        analyzerNameToSubtitleKey.put(AnalyzerTestNameCache.COBAS_TAQMAN, "banner.menu.results.cobas.taqman");
        analyzerNameToSubtitleKey.put(AnalyzerTestNameCache.COBAS_DBS, "banner.menu.results.cobasDBS");
        analyzerNameToSubtitleKey.put(AnalyzerTestNameCache.COBAS_C311, "banner.menu.results.cobasc311");
    }

    @RequestMapping(value = "/AnalyzerResults", method = RequestMethod.GET)
    public ModelAndView showAnalyzerResults(@Valid @ModelAttribute("form") AnalyzerResultsForm oldForm,
            BindingResult result, HttpServletRequest request)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        AnalyzerResultsForm form = new AnalyzerResultsForm();

        request.getSession().setAttribute(SAVE_DISABLED, TRUE);

        String requestAnalyzerType = null;
        if (!result.hasFieldErrors("type")) {
            requestAnalyzerType = oldForm.getType();
        }

        form.setType(requestAnalyzerType);

        AnalyzerImporterPlugin analyzerPlugin = pluginAnalyzerService.getPluginByAnalyzerId(getAnalyzerIdFromRequest());
        if (analyzerPlugin instanceof BidirectionalAnalyzer) {
            BidirectionalAnalyzer bidirectionalAnalyzer = (BidirectionalAnalyzer) analyzerPlugin;
            form.setSupportedLISActions(bidirectionalAnalyzer.getSupportedLISActions());
        }

        AnalyzerResultsPaging paging = new AnalyzerResultsPaging();
        List<AnalyzerResults> analyzerResultsList = getAnalyzerResults();
        if (GenericValidator.isBlankOrNull(request.getParameter("page"))) {
            // get list of AnalyzerData from table based on analyzer type
            if (analyzerResultsList.isEmpty()) {
                form.setResultList(new ArrayList<AnalyzerResultItem>());
                form.setDisplayNotFoundMsg(true);
                paging.setEmptyPageBean(request, form);

            } else {
                paging.setDatabaseResults(request, form, getAnalyzerResultItemList(analyzerResultsList, form));
            }
        } else {
            paging.setDatabaseResults(request, form, getAnalyzerResultItemList(analyzerResultsList, form));
            paging.page(request, form, Integer.parseInt(request.getParameter("page")));
        }

        addFlashMsgsToRequest(request);
        return findForward(FWD_SUCCESS, form);
    }

    @RequestMapping(value = "/rest/AnalyzerResults", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    @ResponseBody
    public AnalyzerResultsForm showRestAnalyzerResults(@RequestParam(required = false) String type,
            HttpServletRequest request)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        AnalyzerResultsForm form = new AnalyzerResultsForm();

        request.getSession().setAttribute(SAVE_DISABLED, TRUE);

        form.setType(type);
        if (GenericValidator.isBlankOrNull(type)) {
            return form;
        }
        List<AnalyzerResults> analyzerResultsList = new ArrayList<>();
        try {
            AnalyzerImporterPlugin analyzerPlugin = pluginAnalyzerService.getPluginByAnalyzerId(
                    AnalyzerTestNameCache.getInstance().getAnalyzerIdForName(getAnalyzerIdFromRequest()));
            if (analyzerPlugin instanceof BidirectionalAnalyzer) {
                BidirectionalAnalyzer bidirectionalAnalyzer = (BidirectionalAnalyzer) analyzerPlugin;
                form.setSupportedLISActions(bidirectionalAnalyzer.getSupportedLISActions());
            }
            analyzerResultsList = getAnalyzerResults();
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "showRestAnalyzerResults",
                    "Error loading analyzer results: " + e.getMessage());
            LogEvent.logError(e);
            return form;
        }

        try {
            AnalyzerResultsPaging paging = new AnalyzerResultsPaging();
            if (GenericValidator.isBlankOrNull(request.getParameter("page"))) {
                if (analyzerResultsList.isEmpty()) {
                    form.setResultList(new ArrayList<AnalyzerResultItem>());
                    form.setDisplayNotFoundMsg(true);
                    paging.setEmptyPageBean(request, form);
                } else {
                    paging.setDatabaseResults(request, form, getAnalyzerResultItemList(analyzerResultsList, form));
                }
            } else {
                paging.setDatabaseResults(request, form, getAnalyzerResultItemList(analyzerResultsList, form));
                paging.page(request, form, Integer.parseInt(request.getParameter("page")));
            }
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "showRestAnalyzerResults",
                    "Error processing analyzer results for display: " + e.getMessage());
            LogEvent.logError(e);
            // Return what we have — results loaded but display processing failed
            form.setResultList(new ArrayList<AnalyzerResultItem>());
            form.setDisplayNotFoundMsg(true);
        }

        addFlashMsgsToRequest(request);
        return form;
    }

    private List<AnalyzerResultItem> getAnalyzerResultItemList(List<AnalyzerResults> analyzerResultsList,
            AnalyzerResultsForm form) {
        /*
         * The problem we are solving is that the accession numbers may not be
         * consecutive but we still want to maintain the order So we will form the
         * groups (by analyzer runs) by going in order but if the accession number is in
         * another group it will be boosted to the first group
         */
        boolean missingTest = false;
        List<AnalyzerResultItem> analyzerResultItemList = new ArrayList<>();
        List<List<AnalyzerResultItem>> accessionGroupedResultsList = groupAnalyzerResults(analyzerResultsList);

        int sampleGroupingNumber = 0;
        for (List<AnalyzerResultItem> group : accessionGroupedResultsList) {
            sampleGroupingNumber++;
            AnalyzerResultItem groupHeader = null;
            for (AnalyzerResultItem resultItem : group) {
                if (groupHeader == null) {
                    groupHeader = resultItem;
                    setNonConformityStateForResultItem(resultItem);
                    if (FormFields.getInstance().useField(Field.QaEventsBySection)) {
                        if (resultItem.getAnalysisId() != null) {
                            resultItem.setNonconforming(getQaEventByTestSection(
                                    analysisService.getAnalysisById(resultItem.getAnalysisId())));
                        }
                    }
                }
                resultItem.setSampleGroupingNumber(sampleGroupingNumber);

                // There are two reasons there may not be a test id,
                // 1. it could not be found due to missing mapping
                // 2. it may not be looked for if the results are read
                // only
                // we only want to capture 1.
                if (GenericValidator.isBlankOrNull(resultItem.getTestId()) && !resultItem.isReadOnly()) {
                    groupHeader.setGroupIsReadOnly(true);
                    missingTest = true;
                } else if (resultItem.getIsControl()) {
                    groupHeader.setGroupIsReadOnly(true);
                }

                analyzerResultItemList.add(resultItem);
            }
        }

        form.setDisplayMissingTestMsg(Boolean.valueOf(missingTest));
        return analyzerResultItemList;
    }

    private void setNonConformityStateForResultItem(AnalyzerResultItem resultItem) {
        boolean nonconforming = false;

        Sample sample = sampleService.getSampleByAccessionNumber(resultItem.getAccessionNumber());
        if (sample != null) {
            nonconforming = QAService.isOrderNonConforming(sample);
            // The sample is nonconforming, now we have to check if any sample items are
            // non_conforming and
            // if they are are they for this test
            // Note we only have to check one test since the sample item is the same for all
            // the tests

            if (nonconforming) {
                List<SampleItem> nonConformingSampleItems = QAService.getNonConformingSampleItems(sample);
                // If there is a nonconforming sample item then we need to check if it is the
                // one for this
                // test if it is then it is nonconforming if not then it is not nonconforming
                TypeOfSample sampleType = analysisService
                        .getTypeOfSample(analysisService.get(resultItem.getAnalysisId()));
                nonconforming = false;
                for (SampleItem nonConformingSampleItem : nonConformingSampleItems) {
                    if (sampleType.getId().equals(nonConformingSampleItem.getTypeOfSample().getId())) {
                        nonconforming = true;
                        break;
                    }
                }
            }
        }
        resultItem.setNonconforming(nonconforming);
    }

    private List<List<AnalyzerResultItem>> groupAnalyzerResults(List<AnalyzerResults> analyzerResultsList) {
        Map<String, Integer> accessionToAccessionGroupMap = new HashMap<>();
        List<List<AnalyzerResultItem>> accessionGroupedResultsList = new ArrayList<>();

        for (AnalyzerResults analyzerResult : analyzerResultsList) {
            AnalyzerResultItem resultItem = analyzerResultsToAnalyzerResultItem(analyzerResult);
            Integer groupIndex = accessionToAccessionGroupMap.get(resultItem.getAccessionNumber());
            List<AnalyzerResultItem> group;
            if (groupIndex == null) {
                group = new ArrayList<>();
                accessionGroupedResultsList.add(group);
                accessionToAccessionGroupMap.put(resultItem.getAccessionNumber(),
                        accessionGroupedResultsList.size() - 1);
            } else {
                group = accessionGroupedResultsList.get(groupIndex.intValue());
            }

            group.add(resultItem);
        }
        return accessionGroupedResultsList;
    }

    // resolveMissingTests was removed: it mutated the DB on GET requests,
    // used testName (display name) as a cache lookup key, and didn't update
    // readOnly — causing the accept flow to skip mapped results.
    // Test resolution now happens once at FHIR import time.

    private List<AnalyzerResults> getAnalyzerResults() {
        return analyzerResultsService.getResultsbyAnalyzer(getAnalyzerIdFromRequest());
    }

    protected AnalyzerResultItem analyzerResultsToAnalyzerResultItem(AnalyzerResults result) {

        AnalyzerResultItem resultItem = new AnalyzerResultItem();
        resultItem.setAccessionNumber(result.getAccessionNumber());
        resultItem.setAnalyzerId(result.getAnalyzerId());
        resultItem.setIsControl(result.getIsControl());
        resultItem.setTestName(result.getTestName());
        resultItem.setUnits(getUnits(result.getUnits()));
        resultItem.setId(result.getId());
        resultItem.setTestId(result.getTestId());
        resultItem.setCompleteDate(result.getCompleteDateForDisplay());
        resultItem.setLastUpdated(result.getLastupdated());
        resultItem.setReadOnly((result.isReadOnly() || result.getTestId() == null));
        resultItem.setResult(getResultForItem(result));
        resultItem.setSignificantDigits(getSignificantDigitsFromAnalyzerResults(result));
        resultItem.setTestResultType(result.getResultType());
        resultItem.setDictionaryResultList(getDictionaryResultList(result));
        resultItem.setIsHighlighted(!GenericValidator.isBlankOrNull(result.getDuplicateAnalyzerResultId())
                || GenericValidator.isBlankOrNull(result.getTestId()));
        resultItem.setUserChoiceReflex(giveUserChoice(result));
        resultItem.setUserChoicePending(false);

        if (resultItem.isUserChoiceReflex()) {
            setChoiceForCurrentValue(resultItem, result);
            resultItem.setUserChoicePending(!GenericValidator.isBlankOrNull(resultItem.getSelectionOneText()));
        }
        return resultItem;
    }

    private boolean giveUserChoice(AnalyzerResults result) {
        /*
         * This is how we figure out if the user will be able to select 1. Is the test
         * involved with triggering a user selection reflex 2. If the reflex has sibs
         * has the sample been entered yet 3. If the sample has been entered have all of
         * the sibling tests been ordered
         */
        if (!TestReflexUtil.isTriggeringUserChoiceReflexTestId(result.getTestId())) {
            return false;
        }

        if (!TestReflexUtil.testIsTriggeringReflexWithSibs(result.getTestId())) {
            return false;
        }

        Sample sample = getSampleForAnalyzerResult(result);
        if (sample == null) {
            return false;
        }

        List<TestReflex> reflexes = reflexUtil.getPossibleUserChoiceTestReflexsForTest(result.getTestId());

        List<Analysis> analysisList = analysisService.getAnalysesBySampleId(sample.getId());
        Set<String> analysisTestIds = new HashSet<>();

        for (Analysis analysis : analysisList) {
            analysisTestIds.add(analysis.getTest().getId());
        }

        for (TestReflex reflex : reflexes) {
            if (!analysisTestIds.contains(reflex.getTest().getId())) {
                return false;
            }
        }
        return true;
    }

    private Sample getSampleForAnalyzerResult(AnalyzerResults result) {
        return sampleService.getSampleByAccessionNumber(result.getAccessionNumber());
    }

    private void setChoiceForCurrentValue(AnalyzerResultItem resultItem, AnalyzerResults analyzerResult) {
        /*
         * If there are no siblings for the reflex then we just need to find if there
         * are choices for the current value
         *
         * If there are siblings then we need to find if they are currently satisfied
         */
        TestReflex selectionOne = null;
        TestReflex selectionTwo = null;

        if (!TestReflexUtil.testIsTriggeringReflexWithSibs(analyzerResult.getTestId())) {
            List<TestReflex> reflexes = reflexUtil.getTestReflexsForDictioanryResultTestId(analyzerResult.getResult(),
                    analyzerResult.getTestId(), true);
            resultItem.setReflexSelectionId(null);
            for (TestReflex reflex : reflexes) {
                if (selectionOne == null) {
                    selectionOne = reflex;
                } else {
                    selectionTwo = reflex;
                }
            }

        } else {

            Sample sample = getSampleForAnalyzerResult(analyzerResult);

            List<Analysis> analysisList = analysisService.getAnalysesBySampleId(sample.getId());

            List<TestReflex> reflexesForDisplayedTest = reflexUtil.getTestReflexsForDictioanryResultTestId(
                    analyzerResult.getResult(), analyzerResult.getTestId(), true);

            for (TestReflex possibleTestReflex : reflexesForDisplayedTest) {
                if (TestReflexUtil.isUserChoiceReflex(possibleTestReflex)) {
                    if (GenericValidator.isBlankOrNull(possibleTestReflex.getSiblingReflexId())) {
                        if (possibleTestReflex.getActionScriptlet() != null) {
                            selectionOne = possibleTestReflex;
                            break;
                        } else if (selectionOne == null) {
                            selectionOne = possibleTestReflex;
                        } else {
                            selectionTwo = possibleTestReflex;
                            break;
                        }
                    } else {
                        // find if the sibling reflex is satisfied
                        TestReflex sibTestReflex = testReflexService.get(possibleTestReflex.getSiblingReflexId());
                        // TestResult sibTestResult =
                        // testResultService.get(sibTestReflex.getTestResultId());

                        for (Analysis analysis : analysisList) {
                            List<Result> resultList = resultService.getResultsByAnalysis(analysis);
                            Test test = analysis.getTest();

                            for (Result result : resultList) {
                                TestResult testResult = testResultService
                                        .getTestResultsByTestAndDictonaryResult(test.getId(), result.getValue());
                                if (testResult != null && testResult.getId().equals(sibTestReflex.getTestResultId())) {
                                    if (possibleTestReflex.getActionScriptlet() != null) {
                                        selectionOne = possibleTestReflex;
                                        break;
                                    } else if (selectionOne == null) {
                                        selectionOne = possibleTestReflex;
                                    } else {
                                        selectionTwo = possibleTestReflex;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        populateAnalyzerResultItemWithReflexes(resultItem, selectionOne, selectionTwo);
    }

    private void populateAnalyzerResultItemWithReflexes(AnalyzerResultItem resultItem, TestReflex selectionOne,
            TestReflex selectionTwo) {
        if (selectionOne != null) {
            if (selectionTwo == null && !GenericValidator.isBlankOrNull(selectionOne.getActionScriptletId())
                    && !GenericValidator.isBlankOrNull(selectionOne.getTestId())) {

                resultItem.setSelectionOneText(TestReflexUtil.makeReflexTestName(selectionOne));
                resultItem.setSelectionOneValue(TestReflexUtil.makeReflexTestValue(selectionOne));
                resultItem.setSelectionTwoText(TestReflexUtil.makeReflexScriptName(selectionTwo));
                resultItem.setSelectionTwoValue(TestReflexUtil.makeReflexScriptValue(selectionOne));
            } else if (selectionTwo != null) {
                if (selectionOne.getTest() != null) {
                    resultItem.setSelectionOneText(TestReflexUtil.makeReflexTestName(selectionOne));
                    resultItem.setSelectionOneValue(TestReflexUtil.makeReflexTestValue(selectionOne));
                } else {
                    resultItem.setSelectionOneText(TestReflexUtil.makeReflexScriptName(selectionOne));
                    resultItem.setSelectionOneValue(TestReflexUtil.makeReflexScriptValue(selectionOne));
                }

                if (selectionTwo.getTest() != null) {
                    resultItem.setSelectionTwoText(TestReflexUtil.makeReflexTestName(selectionTwo));
                    resultItem.setSelectionTwoValue(TestReflexUtil.makeReflexTestValue(selectionOne));
                } else {
                    resultItem.setSelectionTwoText(TestReflexUtil.makeReflexScriptName(selectionTwo));
                    resultItem.setSelectionTwoValue(TestReflexUtil.makeReflexScriptValue(selectionOne));
                }
            }
        }
    }

    private String getResultForItem(AnalyzerResults result) {
        if (TypeOfTestResultServiceImpl.ResultType.NUMERIC.matches(result.getResultType())) {
            return getRoundedToSignificantDigits(result);
        }

        if (TypeOfTestResultServiceImpl.ResultType.isTextOnlyVariant(result.getResultType())
                || GenericValidator.isBlankOrNull(result.getResultType())
                || GenericValidator.isBlankOrNull(result.getResult())) {

            return result.getResult();
        }

        // If it's readonly or the selectlist can not be gotten then we want the result
        // otherwise we want the id so the correct selection will be choosen
        if (result.isReadOnly() || result.getTestId() == null || result.getIsControl()) {
            return dictionaryService.get(result.getResult()).getDictEntry();
        } else {
            return result.getResult();
        }
    }

    private String getSignificantDigitsFromAnalyzerResults(AnalyzerResults result) {
        if (result.getTestId() == null) {
            return null;
        }

        List<TestResult> testResults = testResultService.getActiveTestResultsByTest(result.getTestId());

        if (GenericValidator.isBlankOrNull(result.getResult()) || testResults.isEmpty()) {
            return null;
        }

        TestResult testResult = testResults.get(0);

        return testResult.getSignificantDigits();
    }

    private String getRoundedToSignificantDigits(AnalyzerResults result) {
        if (result.getTestId() != null) {

            Double results;
            try {
                results = Double.valueOf(result.getResult());
            } catch (NumberFormatException e) {
                return result.getResult();
            }

            String significantDigitsAsString = getSignificantDigitsFromAnalyzerResults(result);
            if (GenericValidator.isBlankOrNull(significantDigitsAsString) || "-1".equals(significantDigitsAsString)) {
                return result.getResult();
            }

            Integer significantDigits;
            try {
                significantDigits = Integer.parseInt(significantDigitsAsString);
            } catch (NumberFormatException e) {
                return result.getResult();
            }

            // not truly significant digits, just decimal places
            return StringUtil.doubleWithSignificantDigits(results, significantDigits);
        } else {
            return result.getResult();
        }
    }

    private String getUnits(String units) {
        if (GenericValidator.isBlankOrNull(units) || "null".equals(units)) {
            return "";
        }
        return units;
    }

    private List<Dictionary> getDictionaryResultList(AnalyzerResults result) {
        if ("N".equals(result.getResultType()) || "A".equals(result.getResultType())
                || "R".equals(result.getResultType()) || GenericValidator.isBlankOrNull(result.getResultType())
                || result.getTestId() == null) {
            return null;
        }

        List<Dictionary> dictionaryList = new ArrayList<>();

        List<TestResult> testResults = testResultService.getActiveTestResultsByTest(result.getTestId());

        for (TestResult testResult : testResults) {
            dictionaryList.add(dictionaryService.get(testResult.getValue()));
        }

        return dictionaryList;
    }

    @Override
    protected String getActualMessage(String messageKey) {
        String actualMessage = null;
        if (messageKey != null) {
            actualMessage = PluginMenuService.getInstance().getMenuLabel(localizationService.getCurrentLocaleLanguage(),
                    messageKey);
        }
        return actualMessage == null ? getActualAnalyzerNameFromRequest() : actualMessage;
    }

    protected String getAnalyzerNameFromRequest() {
        String analyzer = null;
        String requestType = request.getParameter("type");
        if (!GenericValidator.isBlankOrNull(requestType)) {
            analyzer = AnalyzerTestNameCache.getInstance().getDBNameForActionName(requestType);
        }
        return analyzer;
    }

    protected String getAnalyzerTypeNameFromRequest() {
        Analyzer analyzer = analyzerService.get(getAnalyzerIdFromRequest());
        if (analyzer.getAnalyzerType() != null) {
            return analyzer.getAnalyzerType().getName();
        }
        return "";
    }

    protected String getActualAnalyzerNameFromRequest() {
        String requestType = request.getParameter("type");
        return requestType;
    }

    protected String getAnalyzerIdFromRequest() {
        // Prefer ID-based lookup (unambiguous). Fall back to name for legacy URLs.
        String idParam = request.getParameter("id");
        if (idParam != null && !idParam.isBlank()) {
            return idParam;
        }
        String requestType = request.getParameter("type");
        if (requestType != null) {
            Analyzer analyzer = analyzerService.getAnalyzerByName(requestType);
            return analyzer != null ? analyzer.getId() : null;
        }
        return null;
    }

    private void writeErrorResponse(HttpServletResponse response, String safeMessage) {
        try {
            response.setContentType("text/plain");
            response.getWriter().write(safeMessage);
        } catch (Exception writeError) {
            LogEvent.logWarn(AnalyzerResultsController.class.getSimpleName(), "writeErrorResponse",
                    "Failed to write error response body: " + writeError.getMessage());
        }
    }

    private boolean getQaEventByTestSection(Analysis analysis) {
        if (analysis == null) {
            return false;
        }
        if (analysis.getTestSection() != null && analysis.getSampleItem().getSample() != null) {
            Sample sample = analysis.getSampleItem().getSample();
            List<SampleQaEvent> sampleQaEventsList = getSampleQaEvents(sample);
            for (SampleQaEvent event : sampleQaEventsList) {
                QAService qa = new QAService(event);
                if (!GenericValidator.isBlankOrNull(qa.getObservationValue(QAObservationType.SECTION))
                        && qa.getObservationValue(QAObservationType.SECTION)
                                .equals(analysis.getTestSection().getNameKey())) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<SampleQaEvent> getSampleQaEvents(Sample sample) {
        return sampleQaEventService.getSampleQaEventsBySample(sample);
    }

    @RequestMapping(value = "/rest/AnalyzerResults", method = RequestMethod.POST)
    @ResponseBody
    public void showRestAnalyzerResultsSave(HttpServletRequest request, HttpServletResponse response, @Validated({
            Paging.class, AnalyzerResultsForm.AnalyzerResuts.class }) @RequestBody AnalyzerResultsForm form) {

        try {
            AnalyzerResultsPaging paging = new AnalyzerResultsPaging();
            paging.updatePagedResults(request, form);
            List<AnalyzerResultItem> resultItemList = paging.getResults(request);

            for (AnalyzerResultItem item : resultItemList) {
                if (item.getIsAccepted() || item.getIsRejected() || item.getIsDeleted()) {
                    LogEvent.logInfo(this.getClass().getSimpleName(), "showRestAnalyzerResultsSave",
                            "POST item: accession=" + item.getAccessionNumber() + ", test=" + item.getTestName()
                                    + ", testId=" + item.getTestId() + ", readOnly=" + item.isReadOnly() + ", accepted="
                                    + item.getIsAccepted());
                }
            }
            acceptService.acceptAndPersist(resultItemList, getSysUserId(request));

        } catch (LIMSRuntimeException e) {
            LogEvent.logError(e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeErrorResponse(response, "Error saving analyzer results");
        } catch (Exception e) {
            LogEvent.logError("Unexpected error saving analyzer results", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeErrorResponse(response, "Unexpected error saving analyzer results");
        }

    }

    @RequestMapping(value = "/AnalyzerResults", method = RequestMethod.POST)
    public ModelAndView showAnalyzerResultsSave(HttpServletRequest request,
            @ModelAttribute("form") @Validated({ Paging.class,
                    AnalyzerResultsForm.AnalyzerResuts.class }) AnalyzerResultsForm form,
            BindingResult result, RedirectAttributes redirectAttibutes) {
        if (result.hasErrors()) {
            saveErrors(result);
            return findForward(FWD_FAIL_INSERT, form);
        }

        AnalyzerResultsPaging paging = new AnalyzerResultsPaging();
        paging.updatePagedResults(request, form);
        List<AnalyzerResultItem> resultItemList = paging.getResults(request);

        try {
            acceptService.acceptAndPersist(resultItemList, getSysUserId(request));
        } catch (LIMSRuntimeException e) {
            LogEvent.logError(e.getMessage(), e);
            String errorMsg = "errors.UpdateException";
            result.reject(errorMsg);
            saveErrors(result);

            return findForward(FWD_VALIDATION_ERROR, form);
        }

        redirectAttibutes.addFlashAttribute(FWD_SUCCESS, true);
        return findForward(FWD_SUCCESS_INSERT, form);
        // if (GenericValidator.isBlankOrNull(form.getType())) {
        // return findForward(FWD_SUCCESS_INSERT, form);
        // } else {
        // Map<String, String> params = new HashMap<>();
        // params.put("type", form.getType());
        // // params.put("page", form.getPaging().getCurrentPage());
        // params.put("forward", FWD_SUCCESS_INSERT);
        // return getForwardWithParameters(findForward(FWD_SUCCESS_INSERT, form),
        // params);
        // }
    }

    // ── Accept business logic extracted to AnalyzerResultsAcceptServiceImpl
    // (Constitution IV) ──

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "analyzerResultsDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "homePageDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return redirectInsertSuccess();
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "analyzerResultsDefinition";
        } else if (FWD_VALIDATION_ERROR.equals(forward)) {
            return "analyzerResultsDefinition";
        } else {
            return "PageNotFound";
        }
    }

    private String redirectInsertSuccess() {
        // Preserve whichever lookup param was used (id or type)
        String idParam = request.getParameter("id");
        String successUrl = idParam != null ? "redirect:/AnalyzerResults?id=" + Encode.forUriComponent(idParam)
                : "redirect:/AnalyzerResults?type=" + Encode.forUriComponent(request.getParameter("type"));
        if (request.getParameter("page") != null) {
            successUrl += "&page=" + Encode.forUriComponent(request.getParameter("page"));
        }
        if (request.getParameter("searchTerm") != null) {
            successUrl += "&searchTerm=" + Encode.forUriComponent(request.getParameter("searchTerm"));
        }
        return successUrl;
    }

    @Override
    protected String getPageTitleKey() {
        return "banner.menu.results.analyzer";
    }

    @Override
    protected String getPageSubtitleKey() {
        String key = analyzerNameToSubtitleKey.get(getActualAnalyzerNameFromRequest());
        if (key == null) {
            key = PluginMenuService.getInstance()
                    .getKeyForAction("/AnalyzerResults?type=" + request.getParameter("type"));
        }
        return key;
    }

}
