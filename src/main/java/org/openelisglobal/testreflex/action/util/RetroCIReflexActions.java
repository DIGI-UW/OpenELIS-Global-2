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
package org.openelisglobal.testreflex.action.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory.ValueType;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.scriptlet.valueholder.Scriptlet;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;

public class RetroCIReflexActions extends ReflexAction {

    private static ResultService resultService = SpringContext.getBean(ResultService.class);
    private static ObservationHistoryTypeService typeService = SpringContext
            .getBean(ObservationHistoryTypeService.class);
    private static DictionaryService dictionaryService = SpringContext.getBean(DictionaryService.class);
    private static AnalyteService analyteService = SpringContext.getBean(AnalyteService.class);
    private static TestResultService testResultService = SpringContext.getBean(TestResultService.class);
    private static TestService testService = SpringContext.getBean(TestService.class);

    private static final String HIV_D = "HIV D";
    private static final String HIV_2 = "HIV 2";
    private static final String HIV_1 = "HIV 1";
    private static final String HIV_N = "HIV N";
    private static final String HIV_INDETERMINATE = "HIV Indeterminate";
    private static final String HIV_INVALID = "HIV Invalid";
    private static final String HIV_NAME = "hivStatus";
    private static final String VIRONOSTIKA = "Vironostika";
    private static final String INNOLIA = "Innolia";
    private static final String VIRONOSTIKA_OR_INNOLIA = "Vir. or Innolia";
    private static String OBSERVATION_HIV_STATUS_ID;
    private static Analyte ANALYTE_CONCLUSION;
    private static Analyte ANALYTE_CD4_CT_GENERATED;
    private static Analyte ANALYTE_GB_RESULT;
    private static Analyte ANALYTE_LYMPH_PER_RESULT;
    private static Analyte ANALYTE_CD4_PER_RESULT;
    private static TestResult TEST_RESULT_CD4_CALCULATED;

    private static List<Integer> CD4_RESULT_TEST_DEPENDANCIES;
    private static Map<String, String> hivStatusToDictonaryIDMap;

    static {
        ObservationHistoryType observationHistoryType = typeService.getByName(HIV_NAME);
        OBSERVATION_HIV_STATUS_ID = observationHistoryType == null ? null : observationHistoryType.getId();

        hivStatusToDictonaryIDMap = new HashMap<>();

        List<Dictionary> dictionaryList = dictionaryService.getDictionaryEntrysByCategoryNameLocalizedSort("HIVResult");

        for (Dictionary dictionary : dictionaryList) {
            if (dictionary.getDictEntry().equals("HIV1")) {
                hivStatusToDictonaryIDMap.put(HIV_1, dictionary.getId());
            } else if (dictionary.getDictEntry().equals("HIV2")) {
                hivStatusToDictonaryIDMap.put(HIV_2, dictionary.getId());
            } else if (dictionary.getDictEntry().equals("HIVD")) {
                hivStatusToDictonaryIDMap.put(HIV_D, dictionary.getId());
            } else if (dictionary.getDictEntry().equals("Negative")) {
                hivStatusToDictonaryIDMap.put(HIV_N, dictionary.getId());
            } else if (dictionary.getDictEntry().equals("Indeterminate")) {
                hivStatusToDictonaryIDMap.put(HIV_INDETERMINATE, dictionary.getId());
            } else if (dictionary.getDictEntry().equals("Invalid")) {
                hivStatusToDictonaryIDMap.put(HIV_INVALID, dictionary.getId());
            }
        }

        Analyte analyte = new Analyte();
        analyte.setAnalyteName("Conclusion");
        ANALYTE_CONCLUSION = analyteService.getAnalyteByName(analyte, false);
        analyte = new Analyte();
        analyte.setAnalyteName("CD4 percentage count Result");
        ANALYTE_CD4_PER_RESULT = analyteService.getAnalyteByName(analyte, false);
        analyte = new Analyte();
        analyte.setAnalyteName("GB Result");
        ANALYTE_GB_RESULT = analyteService.getAnalyteByName(analyte, false);
        analyte = new Analyte();
        analyte.setAnalyteName("Lymph % Result");
        ANALYTE_LYMPH_PER_RESULT = analyteService.getAnalyteByName(analyte, false);
        analyte = new Analyte();
        analyte.setAnalyteName("generated CD4 Count");
        ANALYTE_CD4_CT_GENERATED = analyteService.getAnalyteByName(analyte, false);

        CD4_RESULT_TEST_DEPENDANCIES = new ArrayList<>();

        Test test = testService.getTestByLocalizedName("GB");
        CD4_RESULT_TEST_DEPENDANCIES.add(Integer.parseInt(test.getId()));
        test = testService.getTestByLocalizedName("Lymph %", Locale.ENGLISH);
        CD4_RESULT_TEST_DEPENDANCIES.add(Integer.parseInt(test.getId()));
        test = testService.getTestByLocalizedName("CD4 percentage count", Locale.ENGLISH);
        CD4_RESULT_TEST_DEPENDANCIES.add(Integer.parseInt(test.getId()));
        test = testService.getTestByLocalizedName("CD4 absolute count", Locale.ENGLISH);

        List<TestResult> resultList = testResultService.getAllActiveTestResultsPerTest(test);
        TEST_RESULT_CD4_CALCULATED = resultList.get(0);
    }

    @Override
    protected void handleScriptletAction(Scriptlet scriptlet) {
        if (scriptlet != null && INTERPERET_TYPE.equals(scriptlet.getCodeType())) {
            String action = scriptlet.getCodeSource();

            if (GenericValidator.isBlankOrNull(action)) {
                return;
            }

            if (action.equals(HIV_INDETERMINATE) || action.equals(HIV_N) || action.equals(HIV_1) || action.equals(HIV_2)
                    || action.equals(HIV_D)) {
                addHIVObservation(action);

            } else if (action.equals("Calc CD4")) {
                finalResult = getCD4CalculationResult(result.getAnalysis().getSampleItem().getSample());
            } else if (action.equals(VIRONOSTIKA_OR_INNOLIA)) {
                Test test = testService.getTestByLocalizedName(VIRONOSTIKA, Locale.ENGLISH);
                if (!test.isActive()) {
                    test = testService.getTestByLocalizedName(INNOLIA, Locale.ENGLISH);
                }
                createReflexedAnalysis(test);
            }
        }
    }

    public Result getCD4CalculationResult(Sample sample) {
        Result calculatedResult = null;
        List<Analysis> analysisList = analysisService.getAnalysisBySampleAndTestIds(sample.getId(),
                CD4_RESULT_TEST_DEPENDANCIES);

        List<Integer> analysisIDList = new ArrayList<>();
        for (Analysis analysis : analysisList) {
            analysisIDList.add(Integer.parseInt(analysis.getId()));
        }

        Result CD4Result = resultService.getResultForAnalyteInAnalysisSet(ANALYTE_CD4_PER_RESULT.getId(),
                analysisIDList);
        Result GBResult = resultService.getResultForAnalyteInAnalysisSet(ANALYTE_GB_RESULT.getId(), analysisIDList);
        Result LymphResult = resultService.getResultForAnalyteInAnalysisSet(ANALYTE_LYMPH_PER_RESULT.getId(),
                analysisIDList);

        if (CD4Result != null && GBResult != null && LymphResult != null) {
            try {
                double result = Double.parseDouble(CD4Result.getValue(true))
                        * Double.parseDouble(GBResult.getValue(true)) * Double.parseDouble(LymphResult.getValue(true))
                        * 0.1;
                result = Math.rint(result);

                calculatedResult = resultService.getResultForAnalyteInAnalysisSet(ANALYTE_CD4_CT_GENERATED.getId(),
                        analysisIDList);
                if (calculatedResult == null) {
                    calculatedResult = new Result();
                    calculatedResult.setResultType("N");
                    calculatedResult.setIsReportable("T");
                    calculatedResult.setTestResult(TEST_RESULT_CD4_CALCULATED);
                    calculatedResult.setAnalyte(ANALYTE_CD4_CT_GENERATED);
                    calculatedResult.setAnalysis(CD4Result.getAnalysis());
                    calculatedResult.setSortOrder(nextSortOrder(CD4Result));
                }

                calculatedResult.setValue(String.valueOf(result));

            } catch (NumberFormatException e) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "getCD4CalculationResult", e.getMessage());
                // no op final result should be null. Handles the case of "XXXX"
            }
        }

        return calculatedResult;
    }

    private String nextSortOrder(Result result) {
        if (result == null || GenericValidator.isBlankOrNull(result.getSortOrder())) {
            return "0";
        }

        return String.valueOf(Integer.parseInt(result.getSortOrder()) + 1);
    }

    private void addHIVObservation(String action) {
        observation = new ObservationHistory();
        observation.setValue(hivStatusToDictonaryIDMap.get(action));
        observation.setValueType(ValueType.DICTIONARY);
        observation.setObservationHistoryTypeId(OBSERVATION_HIV_STATUS_ID);

        finalResult = new Result();
        finalResult.setValue(hivStatusToDictonaryIDMap.get(action));
        finalResult.setResultType("D");
        finalResult.setIsReportable("T");
        finalResult.setAnalyte(ANALYTE_CONCLUSION);
    }
}
