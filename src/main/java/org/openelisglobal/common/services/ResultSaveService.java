/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package org.openelisglobal.common.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.serviceBeans.ResultSaveBean;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.referral.service.ReferralResultService;
import org.openelisglobal.referral.valueholder.ReferralResult;
import org.openelisglobal.result.action.util.ResultUtil;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.service.ResultSignatureService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultSignature;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("prototype")
@DependsOn({ "springContext" })
public class ResultSaveService {
    private static ResultService resultService = SpringContext.getBean(ResultService.class);
    private static TestResultService testResultService = SpringContext.getBean(TestResultService.class);
    private static ResultSignatureService resultSigService = SpringContext.getBean(ResultSignatureService.class);
    private static ReferralResultService referralResultService = SpringContext.getBean(ReferralResultService.class);

    private Analysis analysis;
    private String currentUserId;
    private boolean updatedResult = false;

    public ResultSaveService() {
    }

    public ResultSaveService(Analysis analysis, String currentUserId) {
        this.analysis = analysis;
        this.currentUserId = currentUserId;
    }

    public void setAnalysis(Analysis analysis) {
        this.analysis = analysis;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public List<Result> createResultsFromTestResultItem(ResultSaveBean serviceBean, List<Result> deletableResults) {
        List<Result> results = new ArrayList<>();
        boolean isQualifiedResult = serviceBean.isHasQualifiedResult();

        if (TypeOfTestResultServiceImpl.ResultType.MULTISELECT.matches(serviceBean.getResultType())
                || TypeOfTestResultServiceImpl.ResultType.CASCADING_MULTISELECT.matches(serviceBean.getResultType())) {

            if (!GenericValidator.isBlankOrNull(serviceBean.getMultiSelectResultValues())) {
                JSONParser parser = new JSONParser();
                try {
                    JSONObject jsonResult = (JSONObject) parser.parse(serviceBean.getMultiSelectResultValues());
                    LogEvent.logInfo(this.getClass().getSimpleName(), "createResultsFromTestResultItem",
                            "Parsing multi-select result values: " + jsonResult.toJSONString());

                    List<Result> existingResults = resultService.getResultsByAnalysis(analysis);
                    for (Object key : jsonResult.keySet()) {
                        getResultsForMultiSelect(results, existingResults, serviceBean, (String) key,
                                (String) jsonResult.get(key), isQualifiedResult);
                    }
                    deletableResults.addAll(existingResults);
                } catch (ParseException e) {
                    LogEvent.logDebug(e);
                }
            }

        } else {
            Result result = new Result();
            Result qualifiedResult = null;

            boolean newResult = GenericValidator.isBlankOrNull(serviceBean.getResultId());

            if (!newResult) {
                result.setId(serviceBean.getResultId());
                resultService.getData(result);

                if (!GenericValidator.isBlankOrNull(serviceBean.getQualifiedResultId())) {
                    qualifiedResult = new Result();
                    qualifiedResult.setId(serviceBean.getQualifiedResultId());
                    resultService.getData(qualifiedResult);
                } else if (isQualifiedResult) {
                    qualifiedResult = getQuantifiedResult(serviceBean, result);
                }
            }

            if (TypeOfTestResultServiceImpl.ResultType.DICTIONARY.matches(serviceBean.getResultType())
                    || isQualifiedResult) {
                setTestResultsForDictionaryResult(serviceBean.getTestId(), serviceBean.getResultValue(), result); // support
                // qualified
                // result
            } else {
                List<TestResult> testResultList = testResultService.getActiveTestResultsByTest(serviceBean.getTestId());
                // we are assuming there is only one testResult for a numeric
                // type result
                if (!testResultList.isEmpty()) {
                    result.setTestResult(testResultList.get(0));
                }
            }

            if (newResult) {
                setNewResultValues(serviceBean, result);
                if (isQualifiedResult) {
                    qualifiedResult = getQuantifiedResult(serviceBean, result);
                }
            }

            setAnalyteForResult(result);
            setStandardResultValues(serviceBean.getResultValue(), result);
            results.add(result);

            if (isQualifiedResult) {
                setStandardResultValues(serviceBean.getQualifiedResultValue(), qualifiedResult);
                results.add(qualifiedResult);
            } else if (qualifiedResult != null) { // covers the case where user
                // made change from qualified to
                // non-qualified
                setStandardResultValues("", qualifiedResult);
                results.add(qualifiedResult);
            }
        }

        Collections.sort(deletableResults, new Comparator<Result>() {
            @Override
            public int compare(Result o1, Result o2) {
                return (o1.getParentResult() != null && o2.getId().equals(o1.getParentResult().getId())) ? -1 : 0;
            }
        });

        if (!deletableResults.isEmpty()) {
            updatedResult = true;
        }
        return results;
    }

    private void getResultsForMultiSelect(List<Result> results, List<Result> existingResults,
            ResultSaveBean serviceBean, String key, String value, boolean isQualifiedResult) {
        int groupingKey = Integer.parseInt(key);
        String[] multiResults = value.split(",");

        LogEvent.logInfo(this.getClass().getSimpleName(), "getResultsForMultiSelect",
                "Processing multi-select results for grouping key: {} with values: {}" + groupingKey + ", " + value);

        Arrays.stream(multiResults).forEach(resultAsString -> {
            LogEvent.logInfo(this.getClass().getSimpleName(), "getResultsForMultiSelect",
                    "Processing multi-select result value: {} " + resultAsString);

            // Find existing result using stream
            Result existingResultFromDB = existingResults.stream()
                    .filter(existingResult -> resultAsString.equals(existingResult.getValue())
                            && existingResult.getGrouping() == groupingKey)
                    .findFirst().orElse(null);

            if (existingResultFromDB != null) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "getResultsForMultiSelect",
                        "Found existing result in DB with ID: {} " + existingResultFromDB.getId());

                existingResultFromDB.setSysUserId(currentUserId);
                results.add(existingResultFromDB);
                existingResults.remove(existingResultFromDB);
                LogEvent.logInfo(this.getClass().getSimpleName(), "getResultsForMultiSelect",
                        "Added existing result to results list: {} " + existingResultFromDB.getId());
            } else {
                LogEvent.logInfo(this.getClass().getSimpleName(), "getResultsForMultiSelect",
                        "Creating NEW result for value: {} " + resultAsString);

                Result result = new Result();
                setTestResultsForDictionaryResult(serviceBean.getTestId(), resultAsString, result);
                setNewResultValues(serviceBean, result);
                setAnalyteForResult(result);
                setStandardResultValues(resultAsString, result);
                result.setSortOrder(getResultSortOrder(result.getValue()));
                result.setGrouping(groupingKey);

                results.add(result);
                LogEvent.logInfo(this.getClass().getSimpleName(), "getResultsForMultiSelect",
                        "Created new result with value: {}" + resultAsString);
            }
        });

        /*
         * Handle quantifiable results
         */
        if (isQualifiedResult) {
            if (!existingResults.isEmpty() && serviceBean.getQualifiedResultId() != null) {
                List<Result> removableResults = existingResults.stream()
                        .filter(existingResult -> serviceBean.getQualifiedResultId().equals(existingResult.getId()))
                        .collect(Collectors.toList());

                removableResults.forEach(existingResult -> {
                    setStandardResultValues(serviceBean.getQualifiedResultValue(), existingResult);
                    results.add(existingResult);
                });

                existingResults.removeAll(removableResults);
            } else {
                String[] quantifiableResults = serviceBean.getQualifiedDictionaryId()
                        .substring(1, serviceBean.getQualifiedDictionaryId().length() - 1).split(",");

                Arrays.stream(quantifiableResults).forEach(quantifiableResultId -> {
                    results.stream().filter(selectedResult -> selectedResult.getValue().equals(quantifiableResultId))
                            .findFirst().ifPresent(selectedResult -> {
                                Result quantifiedResult = getQuantifiedResult(serviceBean, selectedResult);
                                setStandardResultValues(serviceBean.getQualifiedResultValue(), quantifiedResult);
                                results.add(quantifiedResult);
                            });
                });
            }
        }

        existingResults.forEach(result -> result.setSysUserId(currentUserId));
    }

    private TestResult setTestResultsForDictionaryResult(String testId, String dictValue, Result result) {
        TestResult testResult;
        testResult = testResultService.getTestResultsByTestAndDictonaryResult(testId, dictValue);

        if (testResult != null) {
            result.setTestResult(testResult);
        }

        return testResult;
    }

    private void setNewResultValues(ResultSaveBean serviceBean, Result result) {
        result.setAnalysis(analysis);
        result.setIsReportable(serviceBean.getReportable());
        result.setResultType(serviceBean.getResultType());
        result.setMinNormal(serviceBean.getLowerNormalRange());
        result.setMaxNormal(serviceBean.getUpperNormalRange());
        result.setSignificantDigits(serviceBean.getSignificantDigits());
    }

    private void setAnalyteForResult(Result result) {
        TestAnalyte testAnalyte = ResultUtil.getTestAnalyteForResult(result);

        if (testAnalyte != null) {
            result.setAnalyte(testAnalyte.getAnalyte());
        }
    }

    private void setStandardResultValues(String value, Result result) {
        if (!(GenericValidator.isBlankOrNull(value) || GenericValidator.isBlankOrNull(result.getValue()))
                && !StringUtil.blankIfNull(value).equals(result.getValue())) {
            updatedResult = true;
        }
        result.setValue(value);
        result.setSysUserId(currentUserId);
        result.setSortOrder("0");
    }

    private String getResultSortOrder(String resultValue) {
        TestResult testResult = testResultService.getTestResultsByTestAndDictonaryResult(analysis.getTest().getId(),
                resultValue);
        return testResult == null ? "0" : testResult.getSortOrder();
    }

    private Result getQuantifiedResult(ResultSaveBean serviceBean, Result parentResult) {
        Result qualifiedResult = new Result();
        setNewResultValues(serviceBean, qualifiedResult);
        setAnalyteForResult(parentResult);
        qualifiedResult.setResultType("A");
        qualifiedResult.setParentResult(parentResult);
        return qualifiedResult;
    }

    public boolean isUpdatedResult() {
        return updatedResult;
    }

    public static void removeDeletedResultsInTransaction(List<Result> deletableResults, String currentUserId) {
        for (Result result : deletableResults) {
            List<ResultSignature> signatures = resultSigService.getResultSignaturesByResult(result);
            List<ReferralResult> referrals = referralResultService.getReferralsByResultId(result.getId());

            for (ResultSignature signature : signatures) {
                signature.setSysUserId(currentUserId);
            }

            resultSigService.deleteAll(signatures);

            for (ReferralResult referral : referrals) {
                referral.setSysUserId(currentUserId);
                referralResultService.delete(referral);
            }

            result.setSysUserId(currentUserId);
            resultService.delete(result);
        }
    }
}
