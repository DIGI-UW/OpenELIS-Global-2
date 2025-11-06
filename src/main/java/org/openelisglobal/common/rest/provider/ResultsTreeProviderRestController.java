package org.openelisglobal.common.rest.provider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.rest.provider.bean.patientHistory.PanelDisplay;
import org.openelisglobal.common.rest.provider.bean.patientHistory.ResultDisplay;
import org.openelisglobal.common.rest.provider.bean.patientHistory.ResultTree;
import org.openelisglobal.common.rest.provider.bean.patientHistory.TestDisplay;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/rest/")
public class ResultsTreeProviderRestController {

    @Autowired
    ResultService resultService;

    @Autowired
    SampleHumanService sampleHumanService;

    @Autowired
    DictionaryService dictionaryService;

    @Autowired
    TestService testService;

    @GetMapping(value = "result-tree", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<ResultTree> getResultTreeArray(@RequestParam String patientId) {
        List<Sample> samples = sampleHumanService.getSamplesForPatient(patientId);
        List<Result> results = new ArrayList<>();
        samples.forEach(sample -> {
            results.addAll(resultService.getResultsForSample(sample));
        });

        List<Analysis> analyses = new ArrayList<>();
        Map<TypeOfSample, Map<TestSection, Set<Result>>> sampleResultMap = new HashMap<>();
        Map<TestSection, Set<TypeOfSample>> testSectionPanelMap = new HashMap<>();

        for (Result result : results) {
            analyses.add(result.getAnalysis());
            TestSection testSection = result.getAnalysis().getTest().getTestSection();
            TypeOfSample sampleType = result.getAnalysis().getSampleItem().getTypeOfSample();
            if (testSectionPanelMap.containsKey(testSection)) {
                testSectionPanelMap.get(testSection).add(sampleType);
            } else {
                Set<TypeOfSample> filteredSamples = new HashSet<>();
                filteredSamples.add(sampleType);
                testSectionPanelMap.put(testSection, filteredSamples);
            }

            if (sampleResultMap.containsKey(sampleType)) {
                if (sampleResultMap.get(sampleType).containsKey(testSection)) {
                    sampleResultMap.get(sampleType).get(testSection).add(result);
                } else {
                    Set<Result> filteredResults = new HashSet<>();
                    filteredResults.add(result);
                    sampleResultMap.get(sampleType).put(testSection, filteredResults);
                }
            } else {
                Map<TestSection, Set<Result>> unitResultMap = new HashMap<>();
                Set<Result> filteredResults = new HashSet<>();
                filteredResults.add(result);
                unitResultMap.put(testSection, filteredResults);
                sampleResultMap.put(sampleType, unitResultMap);
            }
        }

        List<ResultTree> resultTrees = new ArrayList<>();

        for (Map.Entry<TestSection, Set<TypeOfSample>> entry : testSectionPanelMap.entrySet()) {
            List<PanelDisplay> panelDisplays = new ArrayList<>();

            for (TypeOfSample sampleEntry : entry.getValue()) {
                Map<Test, Set<Result>> testResultMap = new HashMap<>();

                for (Result resultEntry : sampleResultMap.get(sampleEntry).get(entry.getKey())) {

                    Test test = resultEntry.getAnalysis().getTest();

                    if (!testResultMap.containsKey(test)) {
                        Set<Result> filteredResults = new HashSet<>();
                        filteredResults.add(resultEntry);
                        testResultMap.put(test, filteredResults);
                    } else {
                        testResultMap.get(test).add(resultEntry);
                    }
                }

                List<TestDisplay> testDisplays = new ArrayList<>();

                for (Map.Entry<Test, Set<Result>> testResultentry : testResultMap.entrySet()) {
                    List<ResultDisplay> resultDisplays = new ArrayList<>();

                    for (Result result : testResultentry.getValue()) {
                        ResultDisplay resultDisplay = new ResultDisplay();
                        String resultType = "";
                        if (result.getTestResult() != null) {
                            resultType = testService.getResultType(result.getTestResult().getTest());
                        }
                        resultDisplay.setValue("");
                        if (resultType.equals("N") || resultType.equals("A") || resultType.equals("R")) {
                            resultDisplay.setValue(result.getValue() != null ? result.getValue() : "");
                        } else if (resultType.equals("D") || resultType.equals("M") || resultType.equals("C")) {
                            if (result.getValue() != null && StringUtils.isNumeric(result.getValue())) {
                                Dictionary dict = dictionaryService.get(result.getValue());
                                if (dict != null) {
                                    resultDisplay.setValue(dict.getDictEntry());
                                }
                            }
                        }
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
                        resultDisplay.setObsDatetime(dateFormat.format(result.getLastupdated()));
                        resultDisplays.add(resultDisplay);
                    }

                    TestDisplay testDisplay = new TestDisplay();
                    testDisplay.setDisplay(testResultentry.getKey().getLocalizedName());
                    testDisplay.setConceptUuid(testResultentry.getKey().getId());
                    testDisplay
                            .setDatatype(testResultentry.getValue().iterator().next().getTestResult() != null
                                    ? testService.getResultType(
                                            testResultentry.getValue().iterator().next().getTestResult().getTest())
                                    : "");
                    testDisplay.setHiNormal(testResultentry.getValue().iterator().next().getMaxNormal());
                    testDisplay.setLowNormal(testResultentry.getValue().iterator().next().getMinNormal());
                    testDisplay.setHighCritical(testResultentry.getValue().iterator().next().getMaxNormal());
                    testDisplay.setLowCritical(testResultentry.getValue().iterator().next().getMinNormal());
                    testDisplay.setLowAbsolute(0.0);
                    testDisplay.setUnits(testResultentry.getKey().getUnitOfMeasure() != null
                            ? testResultentry.getKey().getUnitOfMeasure().getName()
                            : "");
                    testDisplay.setObs(resultDisplays);
                    testDisplays.add(testDisplay);
                }

                PanelDisplay panelDisplay = new PanelDisplay();
                panelDisplay.setDisplay(sampleEntry.getLocalizedName());
                panelDisplay.setSubSets(testDisplays);
                panelDisplays.add(panelDisplay);
            }

            ResultTree resultTree = new ResultTree();
            resultTree.setDisplay(entry.getKey().getLocalizedName());
            resultTree.setSubSets(panelDisplays);
            resultTrees.add(resultTree);
        }

        return resultTrees;
    }

    @GetMapping(value = "test-result-tree", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public PanelDisplay getTestResultTree(@RequestParam String patientId, @RequestParam String testId) {
        Test test = testService.get(testId.trim());
        if (test == null) {
            return null;
        }
        List<Sample> samples = sampleHumanService.getSamplesForPatient(patientId);

        List<Result> results = new ArrayList<>();
        samples.forEach(sample -> {
            results.addAll(resultService.getResultsForSample(sample));
        });

        Map<Test, Set<Result>> testResultMap = new HashMap<>();

        Set<Result> filteredResults;
        for (Result result : results) {
            if (test.equals(result.getAnalysis().getTest())) {
                if (testResultMap.containsKey(test)) {
                    testResultMap.get(test).add(result);
                } else {
                    filteredResults = new HashSet<>();
                    filteredResults.add(result);
                    testResultMap.put(test, filteredResults);
                }
            }
        }

        List<TestDisplay> testDisplays = new ArrayList<>();

        for (Map.Entry<Test, Set<Result>> testResultentry : testResultMap.entrySet()) {
            List<ResultDisplay> resultDisplays = new ArrayList<>();

            for (Result result : testResultentry.getValue()) {
                ResultDisplay resultDisplay = new ResultDisplay();
                resultDisplay.setValue(result.getValue(true) != null ? result.getValue(true) : "");
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
                resultDisplay.setObsDatetime(dateFormat.format(result.getLastupdated()));
                resultDisplays.add(resultDisplay);
            }

            TestDisplay testDisplay = new TestDisplay();
            testDisplay.setDisplay(testResultentry.getKey().getLocalizedName());
            testDisplay.setConceptUuid(testResultentry.getKey().getId());
            testDisplay.setDatatype(
                    testService.getResultType(testResultentry.getValue().iterator().next().getTestResult().getTest()));
            testDisplay.setHiNormal(testResultentry.getValue().iterator().next().getMaxNormal());
            testDisplay.setLowNormal(testResultentry.getValue().iterator().next().getMinNormal());
            testDisplay.setHighCritical(testResultentry.getValue().iterator().next().getMaxNormal());
            testDisplay.setLowCritical(testResultentry.getValue().iterator().next().getMinNormal());
            testDisplay.setLowAbsolute(0.0);
            testDisplay.setUnits(testResultentry.getKey().getUnitOfMeasure() != null
                    ? testResultentry.getKey().getUnitOfMeasure().getName()
                    : "");
            testDisplay.setObs(resultDisplays);
            testDisplays.add(testDisplay);
        }

        PanelDisplay panelDisplay = new PanelDisplay();
        panelDisplay.setDisplay(test.getLocalizedName());
        panelDisplay.setSubSets(testDisplays);
        return panelDisplay;
    }
}
